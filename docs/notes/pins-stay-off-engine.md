# Pins stay off-engine: the read seam and the transaction map are the whole mechanism

- **status**: argued — direction ratified in conversation (Sep 2026);
  supersedes wire-face's `Fiber<Pin>` seam, which is withdrawn
- **evidence held**: demonstration for the sync half (the shipped
  `Simulated` ledger IS the mechanism: capture at `computeIfAbsent`,
  composition at commit's `Footprint.union`); derivation for the rest
- **imports**: none new
- **obligations**:
  - the producer receipt (the cheapest kill): a `Transaction` over a
    wire-shaped source — canned responses carrying validators — whose
    adapter registers the pin at the read seam BEFORE serving answers;
    if capture-at-seam cannot express that source, the claim dies
  - #75's pin stamps consume the persisted-memo case: the stamp lives
    WITH the memo and is compared at reuse — no engine involvement
  - the capture contract stays trust, restated at the seam: the
    validator is minted with-or-before the answers it certifies (the
    Oracle-costume boundary; no signature enforces it)
- **links**: wire-face.md (the write door this proof transports),
  transaction.md, tasks #64 (now decoupled), #75

The claim: a pin never enters the engine — not the package, not the
produce signature, not an ambient scope. The read seam hands data and
pin together (`Pinned<Iterable<Answer>> read(Call)`), the transaction's
probe→pin map captures every touch as a side effect of the read, and
composition is one fold at commit (`Footprint.union`, conflict on
cross-world mismatch). That shipped mechanism is not a placeholder for
a future on-engine design; it is the correct grain, and the widened
produce signature (`Fiber<Pin> produce`) buys nothing over it.

Why the seam-level map is the RIGHT grain, not a compromise: the
footprint a commit must certify includes the reads that justified
ABSENCE. "Lend if no active loans" stands on a read that returned
empty, performed inside a sub-search whose branches all died — no
surviving derivation's lineage contains it. A per-derivation footprint
(pins riding the package, one log per fork) omits exactly the read
that write skew stands on; the map catches it because capture fires at
the probe, indifferent to branch fate. The over-approximation is the
soundness, not the price. (Second, smaller nail: an answer's condition
⊕-folds several derivations, so "the derivation's footprint" is not
even well-defined per answer.)

The old forking objection — "pins cannot ride branches" — was an
artifact of the completion-pin shape, not a law: under pin-at-
completion the pin does not exist yet when emissions fork, so packages
cannot carry it; under the shipped pin-at-read shape they could. They
just should not, per the paragraph above.

The cases that seemed to need on-engine pins, checked:

- **A produce-backed source.** The response arrives validator-with-
  body; the adapter registers the pin at the seam, then serves answers —
  pin-before-read holds, zero-answer probes still pin (capture is at
  the probe, not per row). The produce signature never touches a pin.
- **Tabled replay.** Tables are per-solve and a solve reads through
  one transaction, so the masters' base reads hit the same seam as
  everyone else's — the map already holds them; replay re-serves
  answers whose reads were captured when they actually happened.
  There is no cross-transaction table to propagate pins through.
- **Persisted memos** — the one genuine cross-world case — store
  their pin stamp with the memo (#75), compared at reuse. Off-engine
  by its own design.
- **Cross-world staleness detection** is `Footprint.union`'s refusal —
  it lives in the map fold already.

What this kills: the `Fiber<Pin> produce` seam, the pin-at-root-
producer ruling, union-of-completions-upward-through-layers, and the
scope-as-join capture mechanism (an attribution problem that only
exists if produces own their pins — they do not). What it frees:
task #64 (`Fiber.external`) is no longer load-bearing for the pin
story; its motivation reduces to non-blocking wire mechanics, judged
on its own schedule.
