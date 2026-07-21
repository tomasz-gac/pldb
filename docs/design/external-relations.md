# External relations — the backend contract, in and out

**STATUS: DESIGN SKETCH (July 2026, derived with the human across one long
conversation). Nothing built. This is the SUBSTRATE that `deferred-lookups.md`
and `table-constraints.md` turn out to be two questions about (§8): both are
"when, and with how much bound, do you call the backend." Companions:
`table-constraints.md` (rows as a narrowing domain), `deferred-lookups.md`
(defer the probe), `query-planning.md` (the pricing this feeds),
`logic/docs/design/constraint-kernel.md` (the store protocol).**

---

## 1. The idea — one two-method contract, in-memory is one implementation

The engine's READ side needs exactly two operations from a relation's backing,
both over the same input `(relation, bound properties)`:

- **enumerate** — the matching rows, for EXECUTION (today's `Relation.exists`);
- **count** — how many match, for PLANNING (today's `Database.estimate`).

pldb already HAS both — they are just methods on the in-memory `Database`.
The move is to LIFT them onto an interface (`FactSource`) that any backing
implements: in-memory index, SQL, a REST poll, a user's hand-rolled Java. The
in-memory database stops being privileged core and becomes one implementation
of the same two methods. Nothing above the line — no derived relation, no query
— knows which backend answered.

```
interface FactSource {
    Stream<Row> enumerate(Relation r, BoundPattern bound);   // execution
    Estimate    count(Relation r, BoundPattern bound);       // planning
    Set<Mode>   supportedModes(Relation r);                  // capability
    PricingCost pricingCost(Relation r);                     // what count() itself costs (§4)
}
```

## 2. Why exactly two operations

Not one, because they serve two different consumers with the same input:
enumerate feeds the SOLVER, count feeds the OPTIMIZER (store-sighted pricing
prices a relation by selectivity WITHOUT materializing it). That split is why
pldb already carries both. Everything else — indexes, storage, remote fetch,
caching — is implementation detail behind these two.

This is also the completeness argument: if `{enumerate, count}` suffice for the
in-memory case (and they do — that is all `exists`/`estimate` ever were), they
are the proven-minimal read surface. In-memory pldb becomes the REFERENCE
implementation that keeps the contract honest.

## 3. The input is a mode signature; the descriptor gates it

`BoundPattern` = `(relation, which properties are bound, their values)` — the
binding pattern with values filled in. So the contract's input type carries the
MODE, and `supportedModes` declares which patterns a backend can answer:
in-memory and SQL support all; a REST source supports the few its endpoints
expose. The planner consults `supportedModes` and routes around what a source
cannot answer, rather than calling `enumerate` and getting a failure. Two
OPERATIONS do the work; one static DESCRIPTOR gates which the engine may ask.

(This "a relation declares its modes" is the recurring abstraction — it also
governs a fragment's tabling args and, §7, the generated endpoints. One concept,
three faces; build it once, on the relation.)

## 4. The asymmetry that bites: count is not always cheap

`enumerate` is always POSSIBLE (maybe slow). `count` is not always cheaply
possible: most REST APIs have no count endpoint, so "how many match?" would mean
paging through everything. That is exactly where a source's poverty hits the
PRICING path. So `count` returns an ESTIMATE OR "unknown/large," never a
guaranteed true count — and the free lunch is that the optimizer already treats
an unpriceable relation as ∞ → a BARRIER that holds position, identical to an
incomplete tabled call (`optimizer.md`, the ∞→exact transition). A source that
cannot count says ∞, the optimizer refuses to reorder across it, correctness is
preserved; it just misses selective-first placement. "Can't count" degrades into
machinery already built.

So `count` carries a COST CLASS, not just an answer — the descriptor's other
static fact (`pricingCost`): **FREE** (in-memory index probe), **PRICED** (a
real round-trip — SQL `COUNT(*)`), **UNAVAILABLE** (the ∞ case above). The
class matters because RECURSION RE-PRICES: the ambient optimizer re-plans each
unfolded body (`optimizer.md` §4a), and each re-planning calls `count` per
conjunct — against a PRICED source that is a round-trip per conjunct PER
RECURSION LAYER, planning costing more than execution.

The consumer is the **re-pricing dial** — three positions, the default chosen
per source by its cost class:

1. **Re-price every forcing** — FREE sources. Catches value-level skew within
   one binding pattern (`x=Tom` hits a 3-row bucket, `x=Alice` a 30k one);
   costs nothing, so a memo would buy nothing.
2. **Memo per adornment** — PRICED sources. Cache the sorted order keyed by
   (body, boundness pattern); re-price exactly when boundness CHANGES — the
   trigger that matters, because a more-bound call's prices only DROP (the
   subset property) but drop NON-UNIFORMLY, so the relative order can flip.
   Value-skew within one pattern is deliberately flattened: catching it costs
   a round-trip per layer.
3. **Subsumption reuse** — UNAVAILABLE sources. Reuse the nearest more-general
   cached plan (SubsumptionMap retrieval); everything prices ∞ anyway, so
   there is nothing to sort by and reuse is lossless.

All three are sound by confluence — a stale order is only ever SLOW, never
wrong — which is what licenses a dial instead of a fixed rule. It also settles
the adornment-memo question structurally for the common cases: FREE sources
never needed the memo, PRICED sources need it by construction; only the tuning
between positions is left to the benchmark.

## 5. Two source shapes — pull vs materialize, chosen per source

The `FactSource` interface admits two implementation shapes, with opposite
consistency/memory profiles. The framework offers both; the user picks by the
source's size and churn.

- **PULL (on-demand):** `enumerate` probes the source when called. Fresh, but
  mode-limited and per-call latency (this is `deferred-lookups.md` — defer until
  bound, then probe). Right for big or rarely-queried sources.
- **MATERIALIZE (poll):** a background loop — the USER's code — fetches facts and
  asserts them via `withFacts`; the engine queries the resulting in-memory
  relation normally, and the existing `Trigger`/`FactsChanged` machinery is the
  update hook. Fast queries, stale between polls, O(source) memory. Right for
  small, hot, reference data.

The framework owns the fact store and the update API; the user owns the
ACQUISITION loop (auth, retries, cadence, backoff — inherently domain-specific,
wrongly opinionated if the framework dictates it). The one concrete adapter
worth SHIPPING is the engine-to-engine one (§7): a pull source that consumes
another pldb service's generated endpoints, making composition among your own
services turnkey. Everything else is a user implementation of the SPI.

## 6. Consistency — coherence, never recency

"What the other service actually has right now" is physically unavailable across
a network: latency and parallelism mean there is no shared "now" to be current
with. So the honest guarantee is CONSISTENCY, not recency — the stance every
MVCC/snapshot-isolation system takes. Two properties, kept distinct:

- **Stability** — the facts do not move UNDER a running query. Free, from pldb's
  per-solve immutability. Always achievable, any source.
- **Point-in-time atomicity** — all facts reflect ONE instant. Only as good as
  the source's own read atomicity: a SQL transaction gives it; N separate REST
  calls smear it across the poll window (stable, but not single-instant).

**The solve is both the memory boundary and the consistency boundary, and that
is correct, not a compromise.** Pull results are cached in the per-solve Package
(the same scope tabling already caches answers in), so within a solve every pull
is stable and mutually coherent — a solve-local snapshot — at O(query footprint)
memory, freed when the solve ends. Across solves you re-pull. This is right
because a QUERY is the unit of consistency: when several facts must agree, they
belong in one query; snapshot isolation guarantees per-transaction, not
cross-transaction, for the same reason. So on-demand pull gets bounded memory
AND per-query coherence — you do NOT have to materialize everything to stay
consistent. Materialization is then an optimization for the small hot sources,
not the only path to consistency.

Residual dial, not a default: a short-TTL cross-solve cache would let a hammered
endpoint reuse a pull across solves, trading recency back for reuse — a
per-source freshness knob, real and tunable.

## 7. The outbound dual — endpoint generation, not a protocol

**DEFERRED — captured, not next.** Endpoint generation is real but low-payoff
now: the framework matters (vert.x and spring both, so the generator is not
trivial), there is a lot to get wrong, and nothing downstream needs the outbound
edge yet. Recorded here so the symmetry is not lost; built only once the inbound
seam (§10 steps 1–3) has paid for itself. The read side is what earns its keep
first.

The same mode signature, pointed OUTWARD, generates a conventional REST surface.
Each `(relation, exposed-mode)` pair becomes one ordinary endpoint:

```
mode [address bound]  ->  GET /property?address=...
mode [agent bound]    ->  GET /property?agent=...
mode [price bound]    ->  GET /property?price=...     <- a BACKWARD query
```

The last runs the relation backward — price in, addresses out — but to the
client it is a normal query param on a normal endpoint. Multidirectionality
becomes INVISIBLE to the caller: they see HTTP, never inversion. So you get the
"expose multidirectionality" win with zero client adoption cost, because you
emit exactly what every HTTP client already speaks — no bespoke protocol to
standardize. The mode signature IS the endpoint catalog; adding an exposed
direction = generating an endpoint.

Composition surfaces here as a SERVER-authoring benefit: you write a relation
spanning several sources, mark it exposable, and the generator emits ordinary
endpoints for it; the client calls a URL and never sees the federation. Clients
get simplicity, you get composition-as-conjunction. Two honest edges: the mode
signature is richer than bound/free (an FD-backward endpoint needs a DOMAIN
bound too, e.g. `priceBand?band=premium&min=..&max=..`, and generation exposes
the modes the author MARKS, not every possible one); and generated REST is
natural for FLAT relational rows — deeply nested responses drift toward
GraphQL-shaped output, which is a richer serializer, not this layer's job.

## 8. What this subsumes

- **`deferred-lookups.md`** = WHEN, and with how much bound, do you call
  `enumerate` (defer until a supported mode's bindings arrive).
- **`table-constraints.md`** = what to do with the rows `enumerate` returned —
  they are a candidate DOMAIN, narrowed locally by the remaining constraints,
  no further calls.

Both stop being separate designs and become two phases of one remote lookup —
fetch, then refine — riding the one `FactSource` contract. A REST-backed
relation uses BOTH: deferral picks the moment and the mode, the returned rows
become an in-memory candidate set the constraint narrowing refines.

## 9. Boundaries

- **Reads only.** `enumerate`/`count` are the read contract. Asserting
  conclusions back to a source is effectful and belongs in the imperative shell
  (Out of the Tar Pit's accidental-state tier), not the query engine.
- **No TCLP.** A tabled call refuses active constraint stores
  (`assertNoConstraints`), so one relation is EITHER a narrowing source
  (table-constraints) OR tabled-recursive, not both — a recursive closure that
  also wants source-narrowing is the blocked case (`logic/docs/design/
  tabled-constraints.md` sketches the crossing). Narrowing XOR tabled-recursion
  per relation; the two rarely coincide.
- **Aggregation is preserved BY CONSTRUCTION** if a source's labelling
  enumerates unlabelled candidates AT REIFY TIME, exactly as FD does — then
  `count`/`sum`/`max`/`min` over a source-backed relation work through the same
  reify-labels-and-branches path FD already proves. Keep that discipline when
  building table-constraints.
- **REST partiality.** A REST-backed relation is a relation with FEWER legal
  directions (its API's modes); the planner must know which, and refuse the
  rest loudly. This is capability, not a bug to fix.
- **Consistency is per-query, source-limited** (§6): stable always,
  single-instant only if the source is, recent never.

## 10. Build order

The seam first, exposure and ergonomics later. The `FactSource` contract is
defined over the EXISTING `Property` columns — it needs no struct codegen, no
new authoring surface. So the first three steps stand alone; everything after is
convenience or reach.

1. **Define the seam on the columns** — the `FactSource` SPI: `enumerate` /
   `count` / `supportedModes` over a `BoundPattern` of `Property` columns. Just
   the interface and its contract; the columns are what pldb already has.
2. **In-memory `Database` as the reference `FactSource`** — re-express today's
   index-backed `Database` AS an implementation of the seam, proving the contract
   complete against the one backend that already works. No behaviour change.
3. **SQL `FactSource` with a hand-written row-mapper** — `enumerate` →
   parameterized `SELECT ... WHERE bound`, `count` → `SELECT COUNT`, all modes
   supported. Map rows to `Fact`s by hand (one small mapper per relation). Estate
   on Postgres with its DERIVED relations UNCHANGED is the proof the layering
   holds — the real, first external backend.
4. **Solve-local pull cache** — a Package store caching `enumerate` results for
   the solve (§6), giving on-demand pull its per-query consistency.
5. **REST pull source** — access-pattern declarations, deferral, count-as-∞,
   partiality (the mode-limited case; the messy inbound backend).

Later, once the seam has paid for itself:

- **Struct-driven mapping** — the hand-written row-mapper of step 3 is the thing
  `@Relational` automates; promote to generated mappers only when hand-mapping
  grates (`relational-structs.md`).
- **Endpoint generation** (§7) — the outbound dual; deferred as premature (see
  §7).
