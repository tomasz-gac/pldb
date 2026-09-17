# Logical names are namespaced; physical names are the source's mapping

- **status**: BUILT for identity (Sep 2026, corrected design): the
  namespace is an IDENTITY FIELD on RelationN, never part of the
  physical name — `relation(Rules.class, "loan")` divides tabling,
  knowledge, and store keys while getName() stays "loan", so tables,
  DDL, mark answers, and pin headers are untouched and the SQL convention
  needs no quoting. The bare-string door is REMOVED; all defining
  methods adopted. The mapping half stays parked — trigger: the first
  physical rename. SUPERSEDED in this note's original text: the
  dotted-name spelling ("Rules.loan" as the name) — wrong because it
  leaked identity into every physical boundary
- **evidence held**: none
- **imports**: Datomic's namespaced idents and Prolog's module-qualified
  predicates named as precedent only
- **obligations**:
  - the namespaced builder door: `Literal.relation(Rules.class, "loan")`
    → logical name `"Rules.loan"` — explicit, zero reflection, stable
    across JVMs, readable in every log, pin header, and mark row; the
    bare-string door stays for single-vocabulary programs
  - `MappingAnswerSource` (or per-source mapping equipment in the
    withCodec pattern): logical relation → physical table / endpoint
    path, registered source-side; the SQL name convention becomes the
    DEFAULT mapping, not the identity
  - the recorded hazard receipted: two value-DISTINCT relations sharing
    a bare name collide at every name-keyed boundary — one watermark
    mark row (false conflicts between unrelated modules), one flush
    table (schema error or corruption), one pin-header key — no shape
    agreement required, just the string
  - the full-identity hazard receipted: two modules minting the SAME
    name+columns silently merge tables in tabling and dedup constraint
    knowledge across semantically distinct relations
  - the wire spelling: pin headers and REST paths carry the namespaced
    logical name; the endpoint mapping is the server source's
- **links**: wire-face.md (names as public keys), persist.md (value
  identity routing), the codec ruling (backend concerns register
  source-side — this note is that ruling applied to NAMING)

The relation's name is doing two jobs today and conflating them: it is
the LOGICAL IDENTITY (what tabling keys, what knowledge dedups by, what
the single-mint function-builder discipline guarantees is stated once)
and it is the PHYSICAL ADDRESS (the SQL table, the REST path, the
watermark row — every boundary keys by the bare string). One vocabulary,
one author, the conflation is free. Two composed relation libraries and
it breaks twice over: same-name-same-columns merges tables silently
(the worst failure: quiet wrongness), and same-name-DIFFERENT-columns —
value-distinct in the engine — still shares every name-keyed boundary
key, bouncing unrelated modules' commits off one mark row and routing
two schemas into one table.

The claim: split the jobs. Logical names are NAMESPACED — the Java
package lesson, and exactly Datomic's answer to exactly this wall
(:loan/member) — via a class-qualified builder door, so program-wide
uniqueness rides the language's own namespace discipline and stays
human-readable at every boundary. Physical names are the SOURCE's
MAPPING — the codec ruling applied to naming: each backend spells the
logical name its own way (table `loan`, path `/loans`), with today's
name convention demoted to the default mapping. Together they solve the
original scenario cleanly: one logic-layer relation, run over REST or
into a DB on demand, schema consistency being each source's declared
business.

What it does NOT buy: identity from method references (no equality
contract — a bound reference is a fresh object per evaluation; refused
permanently), and random names (dead on arrival: names are public keys
across processes — pin headers, watermark answers, endpoints — and
random-per-JVM breaks every cross-process agreement and every log).

Cheapest kill: compose two toy relation modules sharing a bare name in
one solve and watch both hazards fire (the merged table, the shared
mark row); then show the namespaced door + default mapping making both
unrepresentable. If the namespaced spelling cannot cross the SQL
boundary cleanly (dots in table names) without the mapping layer, the
two obligations are load-bearing TOGETHER — which is the design's
claim, so that outcome confirms rather than kills; the true kill is
namespacing breaking the single-mint ergonomics badly enough that
authors won't use it.
