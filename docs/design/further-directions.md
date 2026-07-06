# Further directions — captured, not designed

Ideas discussed and worth remembering, each a paragraph. None is designed in detail; write
a real design note (like the siblings in this directory) before building any of them.

## Weighted facts — probabilistic database + lineage

Give `Fact` an optional semiring weight and pldb becomes the natural first client of
`logic/docs/design/semiring-inference.md`: probability semiring → uncertain facts and
marginal-probability queries (a perception layer writes noisy facts; queries marginalise);
provenance semiring → every answer carries *which facts support it* (lineage/explanation
for free). Provenance semirings were invented for databases (Green et al.), so the theory
maps exactly. Prerequisite: semiring-inference Phases 1–2 in the logic repo. This is the
strongest tie between pldb and the "brain framework" ambition.

## Incremental materialized views (semi-naive Datalog via triggers)

A derived relation = a rule + a `Trigger` maintaining its facts incrementally as base facts
change. The trigger machinery is already the hook and `FactsChanged` is already the delta.
Additions are the easy half (semi-naive evaluation); **deletions are the classic hard part**
(DRed / counting algorithms) — do not improvise them. Turns pldb from fact store into a
Datalog engine with cached inference.

## Standing queries / reactive subscriptions

"Register a query, get notified when its answer set changes." Cheap version: an `Observer`
re-runs the query on relevant `FactsChanged` and diffs. Incremental version falls out of
materialized views. This is the runtime shape of an agent loop: perceive → assert facts →
standing queries fire → act.

## Time-travel / versioning / durability

The database is a persistent structure and `withFacts` already returns a new value — keep
the version chain and snapshots, as-of queries and diffs are nearly free. Durability =
event-source the `FactsChanged` log and replay. High neatness-to-effort ratio; pick it up
whenever it becomes useful.

## Explicitly parked

- **First-argument indexing**: NOT missing — pldb's all-subsets indexing subsumes it (mark
  one column `indexed()` and you have Prolog's behaviour). The only real item there is
  *cost tuning* (declared composite indexes instead of the powerset) if profiling ever
  demands it. See `database-ergonomics.md` "Known non-problem".
- **Neural-symbolic / soft unification**: ruled out of scope by Tom (research-scale).
