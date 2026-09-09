package com.tgac.pldb.transaction;

// ABOUTME: Simulated serialization: a source that proves its reads unmoved and
// ABOUTME: lands the flush in one short transaction of its own.

import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Fact;
import java.util.List;

/**
 * A source that serializes by proof: {@link #pin()} names the
 * snapshot at open. {@link #commit} runs ONE short transaction of the
 * source's own: take the commit lock, prove that the current world
 * still covers what the pin's world said about the footprint, land
 * the facts, record the movement, commit — or answer {@code false}
 * with nothing landed. It cannot run inside the snapshot's
 * transaction: proving "unmoved" requires reading the CURRENT world,
 * which a snapshot by definition refuses to show. Granularity is the
 * implementor's: a coarse source conflicts on any movement, a
 * per-relation source compares only the footprint's relations, a
 * region-aware source matches delta rows. Conservative refusal is
 * always sound.
 */
public interface SimulatedSerialization extends AnswerSource, AutoCloseable {

	Pin pin();

	boolean commit(Pin pin, Footprint read, List<Fact> flush);
}
