# Query planning — goal reordering for faster lookups

**Status:** designed (July 2026, with the human — supersedes the earlier sketch),
NOT implemented. The infrastructure half lives in `logic` (the Optimizer seam),
the planner half here. Build order: logic seam (pins first) → `LookupGoal` +
`estimate()` + static planner → benchmark → only then the dynamic tier.

---

## 1. The idea

In a conjunction of relation lookups, order matters enormously:

```java
parent.exists(gp, p).and(parent.exists(p, lval("Tomek")))   // slow: enumerate ALL parent
parent.exists(p, lval("Tomek")).and(parent.exists(gp, p))   // fast: probe, then probe
```

Same answers, wildly different cost. A planner reorders pure conjunctions of
relation lookups so the most selective goal runs first and each goal's bindings
feed the next (Datalog's sideways information passing).

## 2. Barriers are a CORRECTNESS rule, not conservatism

The planner whitelists goals it owns (`LookupGoal`) and treats everything it
does not recognise as an immovable barrier. Two independent reasons, and the
second is the one that must never be weakened:

1. **Impurity.** `conda`/`condu` commit, `project` inspects bindings —
   reordering changes meaning.
2. **Tabling.** A table entry is keyed on the reified call arguments — its
   boundness pattern. Moving a binder ahead of a tabled call turns one general
   entry (`p(_.0, _.1)`: one master, N slaves sharing its answers) into one
   entry **per binding value** (`p(_.0, alice)`, `p(_.0, bob)`, …): duplicated
   fixpoint computations, and an unbounded table when the binder upstream
   enumerates unboundedly. Variant tabling makes the call pattern semantically
   load-bearing. The optimizer contract, stated once: **a rewrite pass must
   preserve the binding environment at every goal it does not itself own.**
   Barrier partitioning guarantees this — all conjuncts before a barrier
   complete before it runs, whatever their order.

(The sound way to get bindings *into* a recursion is magic sets — reify the
relevant-values set as a derived relation joined inside the body; values land
in answer rows, table keys stay per-mode and bounded by program text. It needs
rules-as-data, which runtime goal trees don't have: a `defer` points at code,
not at a rewritable definition. Out of scope unless pldb ever makes rules
first-class. The degenerate case — the bound arg is invariant through the
recursion — is just "write the filter inside the tabled body by hand",
available today; see also §7.)

## 3. The seam in `logic`: Optimizer / Optimized / Guard

- **`Optimizer`** — a visitor over the goal combinators: overloads for
  `Conjunction`, `Conde`, `Conda`, `Condu`, `NamedGoal`, plus a generic
  `visit(Goal)` fallback that is the extension hook (pldb's planner does its
  `instanceof LookupGoal` there). Dispatch by
  `default Goal accept(Optimizer o) { return o.visit(this); }` on `Goal`, so
  opaque lambdas land in the fallback — untouched by construction.
- **`CascadingOptimizer`** — base class carrying structural recursion +
  normalization. It absorbs the flatten logic currently dead in the
  parameterless `Goal.optimize()` / `Conjunction.optimize()` /
  `Conde.optimize()` (nothing in the solve path calls them — a dangling seam;
  it dies once absorbed). Pin the flattening behavior BEFORE moving it.
- **`Goal.optimize(Optimizer)`** → returns **`Optimized implements Goal`**: a
  wrapper that runs the optimizer at apply time and **re-wraps deferred /
  unrecognized subgoals inward**, so each layer of a recursive unfolding gets
  optimized as it materializes. Apply-time is the only time recursion is
  optimizable at all — construction time sees a `defer` wall.
- **`NamedGoal` is transparent** (recurse inside, preserve the label):
  otherwise turning on tracing would silently turn off planning.
- **`Guard`** — a `@Value` leaf wrapper the visitor never enters. Because
  NamedGoal is transparent, wrapping no longer protects; Guard is the explicit
  opt-out ("I ordered these deliberately") and makes the barrier contract
  testable.
- **Composition**: optimizers compose as an ordered pipeline of passes
  (`Goal → Goal` endofunctions), never by merging visitors.

## 4. The pipeline shape — four single passes, NO fixpoint anywhere

```
normalize  →  unroll(k)  →  normalize  →  plan
```

- **normalize** (flatten nested and/or, prune trivial branches) is one
  bottom-up structural recursion: children normalize first, the parent
  splices — nothing nested survives a single traversal. The recursion IS the
  termination argument; no iterate-until-stable loop. The second normalize is
  an ordinary re-application after unroll splices bodies in, not a drain.
- **unroll(k)** GROWS the tree — fuel-bounded by construction.
- **plan** is a permutation — deterministic given tree + stats, hence
  idempotent: run once.
- Do NOT run the pass set to fixpoint: pass-level iteration is only needed
  when a rewrite creates redexes it cannot see in its own traversal — flatten
  cannot, and the pair that can (factoring/distribution, §8) are mutual
  inverses: cost-directed, not lattice-ordered — a naive drain oscillates.
  Optimization is argmin over an equivalence class, not a fixpoint of a
  rewrite relation (same lesson as logic's `fixpoint-machine.md` §9).

## 5. Static planner (Phase 1)

pldb side:

- **`LookupGoal`** (`@Value`: `Database db, Relation rel, Array<Unifiable<?>> args`)
  replaces the anonymous lambda in `RelationN.relation` — planner-visible, and
  a real `toString` for traces. Each lookup captures its own db, so cost
  estimates need no context threading.
- **`estimate(Relation, boundArgs)`** on `ImmutableDatabase` — the index tries
  already know their bucket sizes; this is exposure, not computation.
- **`PlanningOptimizer extends CascadingOptimizer`**: within each
  barrier-delimited segment of a flattened conjunction, greedy selectivity
  ordering with the simulated bound-set:

```
plan(lookups, boundVars):
    while lookups not empty:
        pick g minimising cost(g, boundVars)   // index bucket size, else relation size
        emit g; boundVars += vars(g)
    // ties: fewer free vars, then smaller relation
```

Recurse into `Conde` branches independently (each disjunct is its own
conjunctive context). "Bound" here = ground at construction or bound by an
earlier lookup in the plan — bindings from OUTSIDE the conjunction are
invisible statically; that gap is the dynamic tier's (§6) and it is the common
case for rules called with bound args.

## 6. Dynamic tier (Phase 3 — benchmark-gated, and a fork)

Facts that shape it:

- A wrong plan is never unsound (pure reordering changes cost only) → caching
  needs no invalidation story.
- The plan depends on the substitution only through the **boundness pattern**
  (adornment) — few distinct patterns per conjunction → **memoize plans per
  adornment**; the per-application cost is only the arg walk, which
  `substituteQueryItems` already pays at execution.
- The arg-walk itself can amortize: "walks to ground" is an **upward-closed**
  fact (substitutions only grow — same monotonicity that makes suspension
  ripeness fire-once). Cache positive results only, branch-scoped by riding
  the `Package` as a plain store (Table/DebugStore pattern). Negative results
  are not stable — never cache "unbound".
- **Tabled bodies self-optimize for free**: the tabling combinator wraps its
  own body in `Optimized` (the barrier binds outsiders, not the owner). The
  master applies the body once per variant, so the plan is computed once per
  table entry, specialized to that variant's boundness — variants ARE
  adornments, and the table is already the memo.
- **Dynamic planning and deferred lookups are SUBSTITUTES** — both exploit
  runtime bindings; build at most one. Deferral has strictly more reach (parks
  across the whole continuation; wakes on bindings from anywhere) and moves
  consumers LATER across barriers — the tabling-safe direction (calls get more
  general, never more specific). The planner covers the intra-segment side;
  choose after the Phase 2 benchmark shows what static misses. `LookupGoal`
  and `estimate()` are shared vocabulary either way (a parked lookup = a
  LookupGoal; "cheap enough to wake" = the same estimate).

## 7. The table-boundary rewrite (MANUAL pattern, not a pass)

`tabled(g).and(g2)` ≡ `tabled(g.and(g2))` as answer sets, ONLY when:

- `g2` is pure;
- **free vars of `g2` ⊆ the tabled call's args** — answers are recorded as
  bindings of the call args and everything else is projected away; push a
  goal mentioning an outer non-arg var inside and its bindings are silently
  lost;
- set semantics suffice — the two sides differ in answer **multiplicity**
  (the table dedups per args-binding), so the rewrite is NOT
  equivalence-preserving under the planned semiring-weighted engine.

Even when sound it is a **cache-granularity trade** (one general reusable
entry vs a lean specialized one nobody else reuses) and it does NOT reach the
recursion levels (recursive calls inside `g` still point at the original
predicate). A global trade the local cost model cannot see → human decision,
documented checklist, never automatic.

## 8. Unrolling (a separate pass) and deeper rewrites

- **`UnrollingOptimizer(k)`**: force bare `defer` suppliers up to depth k,
  splice bodies in — the planner's window then spans layers, and the bound-set
  simulation handles cross-layer dependencies automatically. The layer-k+1
  defer stays a leaf where `Optimized`'s inward wrapping resumes per-layer
  planning (sliding window). Constraints: fuel (unbounded unrolling diverges
  at plan time), forced construction must be pure, and **never unroll through
  a tabled call** — the call node carries variant lookup, reuse and the
  termination argument; tabling IS the commitment not to unfold.
- **Factoring** `(g∧a)∨(g∧b) → g∧(a∨b)` and **distribution** (its inverse):
  sound for pure goals, inspectable via `Conde`, genuinely useful — but they
  are the non-confluent pair of §4; later passes, chosen by cost, never
  drained.

## 9. Phases and acceptance

1. **logic seam**: pin current flatten behavior; `Optimizer` +
   `CascadingOptimizer` (absorbing flatten; parameterless `optimize()` dies) +
   `Goal.optimize(Optimizer)`/`Optimized` + `Guard`. Pins: flatten unchanged;
   optimizer never reorders across a Guard / unrecognized goal; NamedGoal
   transparency.
2. **pldb static planner**: `LookupGoal`, `estimate()`, `PlanningOptimizer`.
   Tests: all existing tests pass with planning on; answer sets identical
   planned vs unplanned on the genealogy fixtures.
3. **Benchmark**: deliberately mis-ordered query over a few thousand facts;
   assert order-of-magnitude reduction in enumerated facts (count probe
   yields, not wall-clock).
4. **Fork** (data-driven): adornment-memoized dynamic planning XOR deferred
   lookups (`deferred-lookups.md`, whose own doc needs re-pointing at the
   shipped kernel: suspend/enforce, not Verdict.run).

## 10. Non-goals

- Reordering anything but recognised pldb lookups (barriers are correctness —
  §2).
- Magic sets / unfold-fold program transformation (needs rules-as-data).
- Running the pass set to a global fixpoint (§4).
- Join algorithms beyond nested-loop-with-index; cross-solve statistics.
