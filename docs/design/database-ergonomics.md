# pldb ergonomics and hardening — design note

**Status:** agreed fixes, NOT implemented. Ordered by value; each phase is independently
shippable. Full test suite (18 tests) must stay green after every phase.

---

## 1. Stop hand-threading the database — let it ride the `Package`

**Problem.** Every query passes `db` explicitly:
```java
parent.exists(db, par, lval("Tomek")).and(parent.exists(db, grandparent, par))
```
Every rule in every consumer forwards a `Database` parameter it never uses itself, and the
database is frozen at goal-construction time (a rule library can't be written once and run
against different databases).

**Fix — the established pattern.** The logic engine already solves exactly this twice:
`Table` (tabling) and `DebugStore` (tracing) are plain `Store`s riding the `Package`'s
constraint-store map — ambient, solve-scoped context, invisible to constraint processing.
Do the same:
- `DbStore implements Store` (copy `com.tgac.logic.debug.DebugStore` verbatim in shape:
  inert `remove`/`prepend`/`contains`, a static `from(Package)` accessor) holding the
  `Database`.
- Relation goals get overloads WITHOUT the `db` parameter that read the `DbStore` from the
  package at application time (inside the goal lambda, where the `Package` is available),
  failing loudly if absent.
- A `solveWith(Database db, Unifiable<T> out)` convenience (or
  `goal.solve(out)` after seeding `Package.withStore(DbStore.of(db))` via `solveFrom`) —
  mirror how `Goal.solve(out, tracer)` seeds the `DebugStore`.

Result:
```java
parent.exists(par, lval("Tomek")).and(parent.exists(grandparent, par))
        .solveWith(db, grandparent)
```

**Decision needed from Tom:** whether to keep the old `exists(db, ...)` overloads or delete
them (all consumers are in-tree; deleting means migrating pldb tests + cuisine + arbitrage +
trading). Do not keep both silently — ask.

---

## 2. Identity hardening — name-based identity silently bites

**Problem.** `Property` equality is `@EqualsAndHashCode` on `(name, indexed)` and
`Relation.indexOf` compares names only. Live consequence in cuisine (`Schema.java`):
```java
public static Property<Integer> WATER    = Property.of("water");
public static Property<Integer> CALORIES = Property.of("water");   // copy-paste bug
```
`CALORIES` and `WATER` are THE SAME property: `fact.get(CALORIES)` silently returns the
water column, both are `Integer`, nothing can catch it. Compounding: `Fact.get` wraps its
cast in `Try -> toJavaOptional()`, so even a type mismatch degrades to a silent
`Optional.empty()`. One level up, the index root key is `(-1, relation.getName())`, so two
relations sharing a name collide in the index.

**Fixes (all small):**
1. `Relations.relation(...)` / `RelationN.of(...)` validate at construction that no two
   properties of one relation share a name — throw `IllegalArgumentException`.
2. `Fact.get` throws on type mismatch (`ClassCastException` propagates) instead of
   returning empty. An absent property may stay `Optional.empty()`.
3. Consider identity (reference) equality for `Property` — they are created once and shared
   as constants, so reference identity is the natural model (same lesson as LVar equality
   in the logic repo). This is the bigger change; ask Tom before doing it, since name-based
   equality may be relied on somewhere.
4. Fix cuisine's `CALORIES` line when touching cuisine (it is currently reading water).

---

## 3. `unique` / `foreignKey` constraints defeat their own index

**Problem.** `Constraint.unique` runs on every insert and does
`db.get(relation, Array.empty())` — a FULL relation scan, collecting every fact's key
values into lists and comparing sizes. Loading n facts is O(n²). `foreignKey` has the same
shape. The index exists precisely to make these probes O(1).

**Fix.** Scope the check to the facts in this `FactsChanged` event: for each added fact,
probe the index for its key values (`db.get(relation, keyQuery)`); uniqueness is violated
iff the probe returns a fact other than the one being added. Same for foreignKey: probe the
pk relation for the fk value. The constraint message loses nothing.

Note: this requires the key properties to be `indexed()`, otherwise the probe degenerates
to the scan anyway — document that `unique`/`foreignKey` want indexed properties, or
validate it at constraint construction.

---

## 4. API consistency sweep (batch these)

- `Database.get` takes vavr `IndexedSeq<java.util.Optional<Object>>`; `withFacts` takes
  `java.util.List`. Pick vavr throughout the `Database` interface.
- `withFacts(...).get()` at 100% of call sites — the `Try` is honest but ergonomically
  lost. Consider throwing on constraint violation, or add a `loadOrThrow` convenience.
  Ask Tom which.
- `TriggerExecution` (BEFORE/AFTER enum) is dead code, referenced nowhere — delete it.
  (Triggers only ever see the post-state; the enum misleads.)
- `System.out.println(db)` in `ImmutableDatabaseTest` — remove (pristine test output).
- `Relations._0.._8` is ~350 lines of hand-maintained near-duplication (post-codegen).
  Acknowledged deliberate debt; arity >8 uses `RelationN` (see cuisine `FLAVOR_PROCESSING`).
  Do not regenerate or hand-extend; do not add `_9`.

---

## Order of work

1. §2 items 1–2 + cuisine fix — smallest, prevents live silent bugs. 
2. §3 — small, removes O(n²) loads.
3. §1 — the ergonomic transformation (needs the Tom decision on old overloads).
4. §4 — batch sweep.

## Known non-problem

The all-subsets indexing (2^k index paths per fact for k indexed columns) is a deliberate
flexibility/cost trade. Leave it alone unless profiling shows insertion cost or memory on a
high-arity relation; the fix then is declared composite indexes instead of the powerset.
