# pldb

The data boundary of the [`logic`](../logic) engine: relations as plain
Java functions over real backends.

## Why

If your facts live in a database, writing *logic* over them usually
means picking a poison:

- **Export and run Datalog** — the engine wants its facts resident, so
  you page the database through the network to discard most of it, and
  the database's indexes and statistics count for nothing.
- **Contort into SQL** — joins are great, recursive CTEs exist, but
  negation through rules, running a relation backwards, symbolic
  answers, and constraint pruning don't; every rule becomes a view you
  now maintain.
- **Accept an ORM** — objects and lazy loading; the logic lives nowhere.

pldb refuses the choice. The engine keeps its full semantics —
unification, tabling with completion detection, negation over completed
extensions, constraint propagation — and the boundary **translates**:

- A probe is a **region**: the bound columns *plus the engine's
  constraint knowledge about the free ones*. Regions compile to WHERE
  clauses, so the database ships only rows the solve can use.
- Shipped regions are **remembered**: a coverage ledger proves when a
  new probe is contained in an already-fetched one, and answers it from
  the local pool — no second round trip, and its cost estimate becomes
  exact, which the engine's query planner feeds on.
- The write face **closes the loop**: a commit must prove that the
  reads which justified it still hold. The double-lend anomaly that
  slips through append-only writes everywhere (write skew — no rows
  collide, so nothing detects the crossing) is refused here, by
  construction, on every backend.

And one decision above all: **a relation is a method.** The schema is
stated once — the signature is the arity, the builder the columns, the
last argument the backing — so the same domain code runs against
in-memory values in tests and PostgreSQL in anger, and cannot tell the
difference. [`apps/library`](../apps/library) is that claim as a test
suite.

## The surface

Declare once:

```java
static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
    return Literal.relation("person")
            .arg("id", id).indexed()
            .arg("name", name)
            .from(db);
}
```

Query in any direction — bound, free, or half of each:

```java
Unifiable<String> name = lvar();
person(db, lval(2), name).solve(name);      // who is #2?
person(db, lvar(), lval("Ada")).solve(...); // which ids are Ada?
```

Facts are the same functions, ground:

```java
Database db = ImmutableDatabase.empty().withFacts(Arrays.asList(
        person(null, lval(1), lval("Ada")).fact(),
        person(null, lval(2), lval("Alan")).fact())).get();
```

Rules are methods calling methods; recursion is the method calling
itself, and tabling makes it terminate — memoized in the solve's own
table, so the second entry into a region consumes the first instead of
re-deriving it:

```java
static Literal reachable(AnswerSource db, Unifiable<Integer> src, Unifiable<Integer> dst) {
    return Literal.relation("reachable")
            .arg("src", src).arg("dst", dst)
            .solving(edge(db, src, dst)
                    .or(defer(() -> {
                        Unifiable<Integer> mid = lvar();
                        return edge(db, src, mid).and(reachable(db, mid, dst));
                    })));
}
```

Bare in a conjunction a literal READS; under `exclude()` it IMPOSES —
negation over the relation's *completed extension*, no modes, no cut,
and it composes with rules:

```java
// a copy with no active loan: negation through a derived relation
static Literal availableCopy(AnswerSource db, Unifiable<Integer> copyId) {
    return Literal.relation("availableCopy")
            .arg("copyId", copyId)
            .solving(copy(db, copyId, lvar())
                    .and(exclude(onLoan(db, copyId))));
}
```

Even `p :- ¬p` is admitted and refused *loudly* — an unstratified
program becomes a cyclic wait the engine names, not a hang.

## The SQL tier

Schema by convention — relation name = table, property names = columns,
values JDBC-representable, columns non-null. No mapping layer, no
annotations:

```java
try (CachingSqlFetch source = CachingSqlFetch.pinned("app", connection)) {
    // the same person() method, now backed by a real table
    person(source, lvar(), name).solve(name);
}
```

What travels to the database is the *region*, not just the pattern.
Constraint knowledge compiles into the WHERE through a per-family
compiler registry (finite domains and nogoods out of the box, your
families pluggable):

```java
Unifiable<Integer> id = lvar();
FiniteDomain.dom(id, EnumeratedDomain.range(2, 5))
        .and(person(source, id, name))
        .solve(name);
// one round trip: SELECT name FROM person WHERE id IN (2, 3, 4)
// — the engine's pruning ran INSIDE the database; comparisons (leq,
// lss) push as <=/<, and nogoods as NOT(...) through the same registry
```

And what lands, stays:

```java
person(source, lvar(), lvar()).solve(...);   // fetches the relation, ONE SELECT
person(source, lval(2), lvar()).solve(...);  // no SQL at all: the ledger proves
                                             // this probe is covered; answered
                                             // from the pool, priced exactly
```

The connection is pinned at construction — a REPEATABLE READ (or
stronger) snapshot, so every read in a solve sees one consistent world.

## The write face

Reading never needs more than a source. *Writing* needs a
`Transaction`: a read face that records every probe, a write face that
stages facts, and a `commit()` that proves the recorded reads unmoved
before anything lands.

```java
try (Transaction tx = AbstractTransaction.over(
        Watermark.over(CachingSqlFetch.pinned("app", connection), pool::get))) {

    // validate against the snapshot — every probe is recorded
    boolean free = copy(tx, lval(1), lvar())
            .and(exclude(onLoan(tx, lval(1))))
            .solve(lvar()).findAny().isPresent();

    if (free) {
        Try<Nothing> landed = tx
                .withFacts(singletonList(loan(null, lval(500), lval(1), lval(100), lval(24)).fact()))
                .get()
                .commit();
        // Conflict inside the Try = a concurrent commit moved a region
        // this transaction read: reopen, re-solve — and the anomaly
        // reappears as an ordinary domain answer ("copy not available")
    }
}
```

The proof is a capability the source declares **by type** — a source
with neither kind has no `Transaction.over` overload, so "committing
against an uncertified source" is not a runtime error but a
compile-time absence:

- **Native serialization** — the backend validates read sets itself
  (an *honest* SERIALIZABLE: PostgreSQL's SSI, two-phase-locking
  implementations). `SerializableSource.postgres(id, connection)`.
  Rented: zero schema demands; the price is trust — a backend whose
  SERIALIZABLE is snapshot isolation in costume (Oracle) must not wear
  the interface, and no code can check that for you.
- **Simulated serialization** — serialization reproduced above the
  backend: a `Pin` names the snapshot, commit proves the recorded
  `Footprint` unmoved under a commit lock, in one short transaction of
  its own. `Watermark` does it on any SQL backend with a working row
  lock (per-relation marks in one private table — a book commit never
  bounces a person writer); `SharedDatabase` does it in pure memory,
  where the persistent value *is* the snapshot:

```java
SharedDatabase store = SharedDatabase.empty();          // one history, in memory
Transaction a = AbstractTransaction.over(store.open("a"));
Transaction b = AbstractTransaction.over(store.open("b"));
// both validate, both stage, first commit wins, second meets Conflict —
// the whole concurrency story, testable in milliseconds, no container
```

Why this matters: two transactions each check "copy 1 is free", each
append a loan — no row collides, so write-write detection sees nothing,
and an append-only system quietly lends one book twice. Certifying
*reads* is the only cure, and both serializations are exactly that.

## What it is not

No ORM, no migrations, no connection pooling, no entity lifecycle —
facts are values, rows are facts, and the transaction is the only
session there is. A research project like its siblings: no release,
APIs move freely, and honesty over polish — the interesting failure
modes are receipted as tests, not documented around.

## Where knowledge lives

- `docs/design/domain-layer.md` — the north star: the engine at the
  data boundary, and what the algebra does and does NOT buy.
- `docs/notes/table-as-the-source.md` — the read seam as built:
  tabling compresses, the solve owns the residence, pldb translates.
- `docs/notes/transaction.md` — the write face as built: the two
  serializations, the protocol, the granularity ladder.
- `docs/design/` — the SQL tier's design line (table constraints,
  query planning, ergonomics).
- Vocabulary is gated by [`logic/docs/glossary.md`](../logic/docs/glossary.md)
  — one gate for all three repos.
