# Table constraints — pldb rows as a constraint store

**STATUS: DESIGN SKETCH (July 2026, the human's observation: "it's almost a
criminal offense that pldb is not a constraint store — aren't we supposed to
minimize branching?"). Nothing built. This is the STRONGER form of
`deferred-lookups.md` (§2 reconciles them) and it CLEARS that sketch's blocker:
logic's constraint-propagation redesign — the prerequisite both docs named — is
DONE (July 2026), so the composition plumbing this needs now exists. Companions:
`logic/docs/design/constraint-kernel.md` (the store protocol this rides),
`logic/docs/design/lattice.md` §5a (branch→data, the two freedoms),
`query-planning.md` (the planner that captures the deferral-only win).**

---

## 1. The offense

`REL.exists(db, args…)` with free variables ENUMERATES: one branch per matching
row, at the earliest possible moment. A join is branch multiplication, and the
only mercy is the optimizer ordering lookups cheapest-first (pldb's `estimate`
feeds it). In the tower's vocabulary (`lattice.md`): pldb has SEMIRING
citizenship only — its disjunctions can be rearranged but never deferred. The
lattice citizenship is missing: branches→data, the domainify move,
pruning-as-data. A candidate row set is a DOMAIN over tuples — "the shadow of an
enumeration nobody ran" — and pldb runs the enumeration every single time.

## 2. Two answers to "minimize branching" — this doc vs. deferred-lookups

There are two points on the branch-minimization spectrum, and this repo now has
a design note for each:

- **DEFER, then branch** (`deferred-lookups.md`): park the lookup as a
  suspension, wake when a variable binds, THEN enumerate the (now-selective)
  probe. Its header commits to `Verdict.run` — "a parked lookup enumerates
  facts, it branches, so it resumes via run, not narrowed." Minimizes branching
  by DELAYING it until more is bound. Wins only when the binding comes from
  OUTSIDE the conjunction (§6 there); the dynamic planner already captures the
  within-conjunction case.
- **NARROW, branch only at labelling** (THIS doc): the candidate row set is a
  DOMAIN. A binding re-narrows it; singleton → COLLAPSE (bind the remaining
  columns, no branch); two posted tables sharing a variable prune each other;
  branching happens only at an explicit `labelo`. Minimizes branching by
  REPLACING it with propagation — GAC over an extensional relation, the table
  constraint of classical CP (STR / compact-table lineage).

The deferred-lookups claim "a lookup can only branch, not narrow" is a MODELING
CHOICE, not a truth: a free column's possible values across the candidate rows
ARE a finite domain, and another constraint restricting that variable should
PRUNE the rows, not fork on them. This doc is the "actually, it narrows"
upgrade. It subsumes deferred-lookups' win (a fully-narrowed singleton is the
deferred probe's best case, reached without ever parking-to-branch) and adds
join propagation on top. Cost: more plumbing (a full narrowing store vs. a
park-and-run suspension). Recommendation: this supersedes deferred-lookups as
the target; keep that doc as the description of the cheaper defer-only slice, or
retire it once this lands.

## 3. The design, on the existing kernel

**One store.** `TableConstraints implements ConstraintStore` — a record-set
store in the `NeqConstraints` mold. A RECORD is one posted lookup:
`(relation, argsTuple, candidates)` where `candidates` is the current row set,
narrowed through pldb's existing indexes by whatever is bound. The store holds
the (immutable, per-solve) `Database`.

**Posting.** A new goal beside `exists` — working name `REL.posted(db, args…)`
— whose `stated` trigger installs the record with its initial narrowing.
`exists` stays as-is: enumerate-now remains a legitimate, sometimes-optimal
choice; the point is that it stops being the ONLY choice. (Naming: `dom`-like,
since it is the same declared fold move — defer this disjunction.)

**Revise.** Bindings arrive → re-narrow the record's candidates via the
indexes (a lookup, not a search). The verdicts, all standard kernel moves:

- candidates empty → fail the revision;
- candidates singleton → COLLAPSE: mint a `Prefix` binding the remaining
  columns (through the chokepoint, per its caller contract) — the FD-collapse
  move on tuples;
- otherwise → keep, with the narrowed set as the record's new own-factor.

Watch chains via the shared `Watches` matcher on the record's free variables.

**Labelling.** `labelo(record-selector)` enumerates the CURRENT candidate set —
the declared branch point, exactly FD labelling's shape. Unlabelled surviving
records reify into answers as constraint records (the `Neq` convention): a
pattern answer whose tuple-membership is part of the answer's truth.

**Pricing — the convergence that makes this worth it.** Store-sighted pricing
(`answers(Package)`) reads a posted record's candidate count, so `labelo`
prices at the LIVE narrowed size — and the optimizer's cheapest-first sort
BECOMES CP's min-domain labelling heuristic, with zero new optimizer code.
Ordering (semiring freedom) and propagation (lattice freedom) finally both
apply to the database.

**Candidate representation, honestly.** Slice 1 materializes the row set and
re-narrows by index re-query on each revise — dumb and correct. STR-style delta
maintenance and compact-table bitsets are benchmark-gated successors; do not
build them without a profile.

## 4. Phase two, SHELVED — the FD bridge

An Integer column's candidate support set can project into an FD domain on the
variable at that position (and FD narrowing can flow back into the row set) —
the kernel's cross-store revision payloads exist for exactly this. TRIGGER: a
real query needing joint FD+table pruning; the estate course's rung-8 what-ifs
("what price moves Elm into the premium band?" — PROPERTY rows × FD on price)
is the natural first customer. Until then, equality/groundness narrowing only.

## 5. Boundaries

- **The tabling wall stands**: a posted table crossing a tabled call is refused
  loudly (`assertNoConstraints`) — same as every constraint store.
- **BigDecimal columns** narrow by equality only (no bounds order in the store;
  no FD over BigDecimal). Integer columns are the phase-two material.
- **Immutable database per solve** — the store snapshots `db` at posting;
  pldb's observers/triggers stay outside the solve, as today.
- **No negation** — a posted table asserts membership, never absence. (Safe
  `not`, the rider deferred-lookups §5 wanted, would ride the SAME store — a
  posted table with an empty candidate set is a satisfiable negation only when
  ground; the groundness check is the shared machinery.)

## 6. Build plan

1. **Slice 1:** `TableConstraints` (post / revise / collapse / fail), `posted`
   goals on `Relations._N`, `labelo`, record reification. Tests: join pruning
   (two posted tables sharing a variable, zero branches before labelling,
   candidate counts observable), singleton-collapse binds remaining columns,
   empty-set fails, unlabelled record survives into the reified answer.
2. **Slice 2:** pricing integration — `labelo` priced by live candidate count;
   test that the optimizer labels the narrowest table first (min-domain, free).
3. **Slice 3:** the FD bridge (§4) — shelved with its trigger.

## 7. What it buys, concretely

The estate course's joins stop multiplying branches (`Offer ⋈ Decision` becomes
mutual pruning); arbitrage's book-crossing becomes candidate-set intersection
instead of level-by-level enumeration; and pldb gains the one store every CP
practitioner expects to find. Method note: this store is the constraint
kernel's FIRST out-of-repo customer, so building it is also the kernel's
COMPOSABILITY AUDIT — if a store cannot be written outside the logic repo
against the public protocol, that is a finding about the kernel, and we want it.
