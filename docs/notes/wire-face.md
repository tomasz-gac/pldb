# wire-face: each relation exposes a write face — one POST, one atomic verdict, proven by pins

- **status**: argued; the write door awaits the Clauseway REST slice —
  the read-side pin story moved to pins-stay-off-engine.md (Sep 2026),
  which decoupled this note from Fiber.external
- **evidence held**: none over the wire; the transported proof is
  shipped (transaction.md)
- **imports**: none new (ETag/If-Match named as comparison only)
- **obligations**:
  - the server side (Tom's shape — the shipped proof projected onto
    HTTP): each relation exposes a WRITE FACE — POST /relation with a
    flat row batch (atomic per POST: one flush, one commit); pins ride
    a custom header as a relation-keyed map ("Clauseway-Pin: loan=..,
    member=..") because the proof is a KEYED conjunction and If-Match
    is a disjunction over one target resource; header values stay
    opaque leaf pins (RFC 7232 opacity IS Pin's javadoc), keys are
    public relation names — relation grain, matching Watermark.covers
    as shipped. Missing header → just post (the empty footprint
    certifying vacuously, aWriteOnlyTransaction... over the wire);
    mismatch → 409 (the Conflict story: re-read, re-solve). THE CLIENT
    CONTRACT: send the pins of the relations the decision READ, not
    just the written one — target-pin-only is mere optimistic locking
    and readmits write skew through disjoint writes over shared reads.
    The transaction lives client-side; the server holds no per-client
    state (per-region pins verified at commit are what make that
    sound). 1NF writes keep the certify question well-formed — a flat
    literal declares its relation. Two tiers by AUDIENCE: v1 exposes
    ONLY the per-relation faces (human-facing — path names the
    relation, body is one row shape, discoverable; event-shaped writes
    are single-relation so this covers the domain). Multi-relation
    atomic writes are the SOURCE-SCOPE write face — POST at the root,
    grouped-by-relation flat rows (the keyed COMPOSITION of the
    per-relation bodies, not a second schema), same pin header: this
    is Writer.withFacts over the wire, machine-written by the
    marshalling layer, never hand-authored — DEFERRED to the
    Clauseway-client/federation work, the first thing that structurally
    needs it.
  - commit over the wire: three outcomes, not two — Landed / Refused /
    Unknown(token), with a client-minted idempotent commit token so
    recovery can ask "did T land?"; async delivers ambiguity later but
    never removes it — the token is protocol, not calling convention.
    The three-way outcome is DISLIKED, an obligation not a ruling;
    dropped from v1. No commit document, no token in v1 — those return
    only if the proof ever goes region-grain over the wire and
    footprints outgrow headers.
  - the read face over the wire (a REST source as AnswerSource): the
    adapter registers the response's validator at the read seam before
    serving rows (pins-stay-off-engine.md's producer receipt); a
    non-blocking implementation waits on Fiber.external (#64) as
    infrastructure, not as semantics
- **links**: pins-stay-off-engine.md (the read-side ruling),
  transaction.md (the proof this transports), persist.md (the
  endpoint story this carries), table-as-the-source.md, task #64

The claim: the shipped commit proof projects onto HTTP without a new
protocol. A write is a POST of flat rows to its relation's face; the
pins the deciding solve read travel as one relation-keyed header; the
server's verdict is the transaction's verdict — landed or 409 — and
the server holds no per-client state, because per-region pins verified
at commit are exactly what make a stateless write door sound. The
transaction stays client-side; the wire carries the proof, not the
session.

What it does NOT buy: capture-before-read stays trust (no wire format
can force a source to mint its validator honestly); commit ambiguity
stays (the wire's third outcome needs a token protocol regardless of
threading — deferred with it).

Cheapest kill: stand the per-relation write face over SharedDatabase
behind an HTTP stub and drive the loan slice's write through it — if
the relation-keyed header cannot carry the footprint the decision
actually read, or the stateless verdict cannot express the Conflict
story, the projection claim dies here before any real server exists.
