# Working on `pldb`

A Prolog-style fact database on top of the `logic` miniKanren engine. Java 8, vavr, lombok.
Auto-loaded every session — read before changing code.

## Workflow rules (same as the logic repo)

- TDD; `mvn test` must end `BUILD SUCCESS` (18 tests today) before any commit/merge.
- Branch per task; `git add` explicit paths (never `-A`); ff-merge to master; push.
- Dependencies build in order: `functional` → `logic` → `pldb` (each `mvn install`), then
  consumers (cuisine, arbitrage, trading). If logic changed, reinstall it first.

## Architecture in five lines

- `Relations._0.._8` — typed relation definitions (hand-maintained since codegen removal;
  arity >8 uses variadic `RelationN` — see cuisine's `FLAVOR_PROCESSING`). `Property<T>` =
  a typed, optionally `indexed()` column. `Fact` = relation + untyped value array.
- `ImmutableDatabase extends AbstractIndexedDatabase` — persistent trie index
  (`ImmutableIndex`) over **every ordered subset of indexed columns** (2^k paths per fact).
- Queries: `relation.exists(db, args...)` produces a `Goal` that probes the index with the
  bound args and unifies candidates.
- Triggers run as post-conditions on `withFacts`/`withoutFacts`, returning `Try<Database>`
  (failure = the update is discarded). `Constraint.unique`/`foreignKey` build on them.

## Landmines

- **Property/Relation identity is NAME-based.** Two `Property.of("water")` are the same
  property. cuisine's `CALORIES = Property.of("water")` is a live copy-paste bug silently
  reading the water column. See `docs/design/database-ergonomics.md` §2 before touching
  identity or `Fact.get` (which also silently swallows type mismatches).
- **`Constraint.unique`/`foreignKey` full-scan the relation on every insert** — O(n²)
  loads. Known; fix is in ergonomics doc §3.
- **Do NOT add `Relations._9`** — arity >8 is what `RelationN` is for (vavr caps at 8).
- All-subsets indexing is a deliberate trade — don't "optimise" it without profiling.

## Design docs (`docs/design/`)

- `database-ergonomics.md` — agreed fixes: db riding the Package (Table/DebugStore
  pattern) instead of hand-threading, identity hardening, index-based constraints, API
  sweep. **Start here.**
- `query-planning.md` — goal reordering by index selectivity (the `Goal.optimize()` seam).
  Self-contained; build before deferred lookups.
- `deferred-lookups.md` — suspend unbound lookups as constraints, wake on binding.
  **Blocked on** logic's constraint-propagation redesign; read its header.
- `further-directions.md` — weighted facts (semiring tie-in), materialized views,
  subscriptions, time-travel. Captured, not designed.
