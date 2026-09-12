package com.tgac.pldb.transaction;

// ABOUTME: Simulated serialization: a source that pins each region at first read
// ABOUTME: and proves the whole footprint unmoved in one short commit transaction.

import com.tgac.logic.tabling.Call;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import java.util.List;

/**
 * A source that serializes by proof, at the footprint's grain:
 * {@link #pin} names one region's version, {@link #commit} runs ONE
 * short transaction of the source's own — take the commit lock, prove
 * every footprint region unmoved since ITS pin, land the facts, record
 * the movement, commit — or answer {@code false} with nothing landed.
 * It cannot run inside a read snapshot: proving "unmoved" requires
 * reading the CURRENT world, which a snapshot by definition refuses to
 * show.
 *
 * <p>THE ORDERING IS THE SOUNDNESS: a pin must be captured atomically
 * with, or BEFORE, the data reads it certifies. Capture-before is sound
 * — if commit finds the pin unmoved, nothing landed between capture and
 * proof, so every later read belongs to the pin's world. Capture-after
 * silently is not: a commit landing between the read and the capture
 * hands back stale data under a fresh pin, and the proof passes on
 * reads the decision never saw. A source reading through one snapshot
 * satisfies the contract for free; a snapshot-less source (a REST
 * resource and its ETag) must mint the validator with the response or
 * before requesting it.
 *
 * <p>Granularity is the implementor's: a coarse source conflicts on any
 * movement, a per-relation source compares only the footprint's
 * relations, a region-aware source matches delta rows. Conservative
 * refusal is always sound.
 */
public interface SimulatedSerialization extends AnswerSource, AutoCloseable {

	Pin pin(Call<Relation> region);

	boolean commit(Footprint read, List<Literal> flush);
}
