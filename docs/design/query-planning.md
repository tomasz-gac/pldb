# Query planning — goal reordering for faster lookups

**Status:** design sketch, NOT implemented. Self-contained (no dependency on other
redesigns). Prototype this BEFORE deferred-lookups.md — a greedy planner captures most of
the same benefit with far less machinery.

---

## 1. The idea

In a conjunction of relation lookups, order matters enormously:

```java
parent.exists(gp, p).and(parent.exists(p, lval("Tomek")))   // slow: enumerate ALL parent
parent.exists(p, lval("Tomek")).and(parent.exists(gp, p))   // fast: probe, then probe
```

The first form enumerates every `parent` fact (n choice points) and filters; the second
probes the index with the bound value, binds `p`, and the next lookup probes again. Same
answers, wildly different cost. A planner reorders pure conjunctions of relation lookups so
the most selective goal runs first and each goal's bindings feed the next (Datalog's
"sideways information passing").

## 2. Why it is SAFE here (and only here)

Reordering conjuncts is sound only for **pure** goals. The logic engine has impure ones
(`conda`/`condu` commit, `project` inspects bindings) — reordering those changes meaning.
But a conjunction of pldb relation lookups is a pure conjunctive query: reordering changes
only enumeration order (which the scheduler re-fairs), never the answer set. Therefore:

- **the planner lives in pldb and applies ONLY to relation-lookup goals it can identify** —
  never reorder arbitrary `Goal`s;
- any non-relation goal in the conjunction acts as a barrier (plan the runs of adjacent
  relation lookups between barriers).

## 3. The seams that already exist

- **`Goal.optimize()`** — `default Fiber<Goal> optimize()` on `Goal`, currently identity;
  `Conde`/`Conjunction` already use it to flatten clauses. The planner is a
  `optimize()` implementation on a pldb conjunction wrapper (or a rewrite pass applied to a
  `Conjunction` whose elements are recognisable relation goals).
- **Free statistics.** The index tries already know their bucket sizes — cardinality
  estimates cost nothing. Expose from `ImmutableDatabase`:
  `long estimate(Relation rel, IndexedSeq<Optional<Object>> boundArgs)` = size of the
  bucket the probe would hit (or relation size when nothing indexed is bound).

To make relation goals plannable, `RelationN.relation(...)` should return a named subtype
(e.g. `class LookupGoal implements Goal { Relation rel; Array<Unifiable<?>> args; ... }`)
instead of an anonymous lambda, so the planner can see the relation and the args.

## 4. Static planner (Phase 1)

Greedy selectivity ordering at optimize/solve time:

```
plan(lookups, boundVars):
    result = []
    while lookups not empty:
        pick g in lookups minimising cost(g, boundVars)
        result += g
        boundVars += vars(g)          // after g runs, its vars are bound
        lookups -= g
    return result

cost(g, boundVars):
    boundArgs = args of g that are ground OR in boundVars
    if any boundArg is indexed:  estimate(rel, boundArgs)     // index bucket size
    else:                        size(rel)                    // full enumeration
```

Notes:
- "bound" at plan time = ground at construction or bound by an earlier lookup in the plan.
  A var bound by unification *outside* the conjunction is invisible statically — that gap is
  what the dynamic planner (Phase 3) or deferred lookups close.
- Ties: prefer fewer free vars, then smaller relation.
- The plan is computed once per solve (the db is fixed during a solve; statistics don't
  drift mid-query).

## 5. Dynamic planner (Phase 3, optional)

Instead of fixing the order up front, choose at runtime: the conjunction goal, when applied
to a `Package`, walks each remaining lookup's args against the *current* substitutions and
runs the cheapest. Strictly better (sees bindings from outside the conjunction), more
machinery (the conjunction becomes a small interpreter). Only build if Phase 1 measurably
under-plans real queries.

## 6. Phases and acceptance

1. `LookupGoal` type + `estimate()` on the database + greedy static planner behind
   `optimize()`. **Tests:** every existing pldb test passes with planning on; answer sets
   are identical planned vs unplanned (property: same `solve` results as a set) on the
   genealogy fixtures.
2. A cost benchmark: a deliberately mis-ordered query (unbound-first) over a few thousand
   facts; assert the planner reduces enumerated facts by an order of magnitude (count facts
   yielded by index probes, not wall-clock).
3. (Optional) dynamic planner.

## 7. Non-goals

- Reordering anything but pldb relation lookups (impure goals make it unsound).
- Join algorithms beyond nested-loop-with-index (no hash joins etc. — YAGNI at this scale).
- Cross-solve statistics or cost-model tuning.
