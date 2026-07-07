# Deferred lookups — run database probes at the last possible moment

> **Re-read through the Suspension lens:** `logic/docs/design/suspensions.md`
> names the concept this design needs — and the feature now EXISTS as
> `Verdict.run` (a parked lookup enumerates facts — it branches — so it resumes
> via run, not narrowed). Implement as: a `DeferredLookups` ConstraintStore
> hosting `Propagator.of(DeferredLookups.class, queryArgs, state ->
> probeIsCheap(state) ? Verdict.run(enumerate(state)) : Verdict.keep())`, with
> the store's `enforceConstraints` as the reify-time flush. No extraction or new
> API is needed.

**Status:** design sketch, NOT implemented, and **BLOCKED on a prerequisite**: this adds a
new `ConstraintStore`, which lands on the constraint-composition limitation documented in
`logic/docs/design/constraint-propagation.md` and `StoreSupport#processPrefix`'s javadoc.
Do not build it before that redesign's Phase 1 (or, at minimum, its Phase 0 guard plus an
explicitly verified pairwise composition). Build `query-planning.md` first — the greedy
planner captures most of this benefit with none of the plumbing.

---

## 1. The idea

A relation lookup with mostly-free arguments (`parent(X, Y)`, nothing bound) currently
enumerates the whole relation eagerly — n choice points, most of which die when later goals
constrain X or Y. Instead, let the lookup **suspend**: park itself as a constraint watching
its free variables, wake when a later goal binds one of them, and only then probe the index
with the (now cheaper, more selective) bound arguments. "Look up at the last possible
moment, so everything that could bind did."

This is the CLP suspension model (Prolog `freeze`/`when`), and the logic engine already has
every piece of the architecture:

| need | existing mechanism |
|---|---|
| wake when a variable gets bound | `ConstraintStore.processPrefix` — runs on every unification with the newly added substitutions |
| a parked lookup as data | a constraint in a store riding the `Package` (Table/DebugStore pattern) |
| flush at the end if never woken | `enforceConstraints` at reify time — exactly where FD does labeling |
| the database to probe | the `DbStore` from `database-ergonomics.md` §1 (prerequisite: db must ride the Package) |

A deferred lookup **is a propagator** in the sense of the constraint-propagation design: its
watched variables are the lookup's free args; its "narrowing rule" is "probe the index and
branch on the matching facts".

## 2. Semantics (what must stay true)

- **Same answer set.** Deferral changes *when* enumeration happens, never *what* is
  derivable. If a suspended lookup is still parked when the search wants to emit an answer,
  `enforceConstraints` forces it: enumerate the remaining candidates then (the labeling
  analogue). A suspended lookup is NOT a satisfied lookup.
- **Completeness.** A lookup must never stay parked forever while the rest of the
  conjunction succeeds — the reify-time flush is mandatory, not optional.
- **Failure still propagates.** If a woken probe finds no facts, that branch fails exactly
  as the eager version would have.

## 3. Deferral policy (when to park vs run)

Run eagerly when:
- all args ground (it's a membership check), or
- at least one **indexed** arg is bound (the probe is already cheap), or
- the relation is small (below a threshold — enumerating 10 facts beats bookkeeping).

Park otherwise (no indexed arg bound and the relation is large). The thresholds should be
constants first; only make them configurable if a real query needs it.

## 4. Sketch

- `DeferredLookups implements ConstraintStore`, riding the Package (inert to other
  constraint processing). Holds parked lookups: `(Relation, Array<Unifiable<?>> args)`.
- `LookupGoal.apply(pkg)`: apply the policy (§3). Park = add the constraint and succeed
  (like `separate` does when it can't decide yet); run = today's behaviour.
- `processPrefix(newSubs, oldPkg)`: for each parked lookup with a newly-bound watched var,
  re-apply the policy — probe now if cheap enough (removing the constraint and branching on
  the facts), else stay parked watching the still-free vars.
- `enforceConstraints(x)`: force every remaining parked lookup (enumerate; this is the
  completeness backstop).
- `reify`: parked lookups should be gone by then (enforce ran); assert empty.

## 5. Rider: safe negation

`not(parent(X, Y))` under closed-world semantics is only sound when the args are ground —
which is *the same machinery*: park the negation until its vars are ground (wake via
processPrefix), then probe and succeed iff no fact matches; force at enforceConstraints
(fail loudly if still non-ground there — an unsafe negation is a program error, not a
silent choice). If deferred lookups exist, safe `not` is a ~small addition and is the main
reason to prefer this over pure planning eventually.

## 6. Honest assessment

- The **dynamic planner** (query-planning.md §5) gets most of the win: it also waits for
  bindings, just within one conjunction. Deferral wins only when the binding comes from
  *outside* the conjunction (another rule, a constraint, tabling) — real but rarer.
- The plumbing cost is a new ConstraintStore + the composition prerequisite. That is why
  this doc is third in line: ergonomics → planner → (propagation redesign) → deferral.
- Interaction with tabling is untested territory (a parked lookup inside a tabled goal's
  continuation); add composition tests like logic's `TraceCompositionTest` before trusting.

## 7. Acceptance tests

- `parent(X, Y) ∧ X = "Wiesław"` enumerates only the probe bucket, not the relation
  (count enumerated facts).
- A lookup never woken (nothing binds its vars) still yields all its answers via the
  reify-time flush — answer set identical to eager.
- Parked lookup + FD/Neq constraints in one query: answers identical to eager (composition).
- `not(...)` fails loudly when forced non-ground; succeeds/fails correctly when ground.
