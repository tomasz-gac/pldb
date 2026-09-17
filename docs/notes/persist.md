# persist: a domain write is a partially-bound solve whose answers land as facts

- **status**: argued (the endpoint exposure rides the REST trigger
  named in wire-face.md; the in-process persist face does not wait
  for it)
- **evidence held**: none
- **imports**: none new (INSERT..SELECT named as comparison only)
- **obligations**:
  - the driving slice: `Library.checkOut` rewritten as one partially-bound
    persist — givens as lvals, dueDay derived, the denial rules as the
    body's guard, zero answers = nothing lands
  - ~~the routing ruling~~ RESOLVED (Sep 2026, flags-in-identity ruling):
    persist demands head/table VALUE IDENTITY. Under the function-builder
    single-mint doctrine one column-chain shapes both, so identity holds
    by construction; a mismatch (same name, different flags) is a bug and
    surfaces as the loud runtime error it deserves — never a convention
    to accommodate.
  - groundness receipt: an answer with a free column or surviving
    residues refuses at persist, by relation and column
  - exhaustion: persist inherits the cold-and-finite contract and its
    refusal family (aggregate closedness)
  - idempotence ruling: answers the table already holds re-insert (schema's
    business, keys) vs persist subtracts existing answers first
- **links**: transaction.md (the commit proof this rides),
  table-as-the-source.md (the read seam), the projected() builder seam;
  graduates into transaction.md's write face or its own reference doc

A domain operation — "member m rents copy c on day d" — reads the world,
derives what follows, and writes the consequence. Today that is
imperative orchestration around the engine: check the denial, compute
the due day, mint the fact, hand it to `withFacts`. The claim: it is ONE
call — `tx.persist(rental(tx, lval(m), lval(c), lval(d), dueDay))` —
where the head's bound args are the request's givens, the free args are
derived by the rule body, and every answer lands as a fact of the head
relation through the transaction's write face. In SQL terms this is
INSERT..SELECT with the engine as the SELECT; in rule terms it is
converting a relation's rule reading into answers for its source reading.
The head names both the answer shape and the write target; body locals
are projected away by the machinery that already exists.

What it buys. The body IS the business logic: guard the body with the
denial rules and a refused request derives nothing — zero answers, no
write, and the denial relation is already the query that explains why.
The transaction arg threads to the body's base relations, so the solve
reads the pinned snapshot, the footprint records exactly the reads the
decision stood on, and the commit proof covers derive-then-write with no
new machinery — the write-skew story is the one transaction.md already
proves. The WriteBuffer's set semantics folds duplicate derivations.
`ground()` on head columns is the operation's required-inputs contract,
stated in the schema — the same declaration a later endpoint exposure
would publish: POST = a partially-bound persist intake, GET = an
AnswerSource, the API surface the schema itself.

What it does NOT buy. No fixpoint: a body reading the relation it
persists into sees the snapshot, so persist is one-step consequence,
deterministic under the pin, never recursive materialization. No
caveated answers: conditional or wide answers refuse — writes stay strict;
persisting caveats is the data-boundary north star's territory. No
cross-batch idempotence: within one persist the buffer dedups; against
answers already stored, the ruling is an obligation above.

Cheapest kill: write the checkOut slice. If the denial-guarded body
cannot deliver the derived dueDay as a ground answer (the addo/domain
wall, or conditions surviving to the answer), the one-call claim dies
and persist degrades to "materialize a ground view", which is still
useful but is a different, smaller idea.
