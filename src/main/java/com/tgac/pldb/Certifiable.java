package com.tgac.pldb;

// ABOUTME: The OWNED certify capability: a source that proves its reads unmoved
// ABOUTME: and lands the flush in one short transaction of its own.

import com.tgac.pldb.relations.Fact;
import java.util.List;

/**
 * A source that certifies its own reads. {@link #pin()} names the
 * snapshot at open. {@link #commit} runs ONE short transaction of the
 * source's own: take the commit lock, prove that the current world
 * still covers what the pin's world said about every footprint, land
 * the facts, record the movement, commit — or answer {@code false}
 * with nothing landed. It cannot run inside the snapshot's
 * transaction: proving "unmoved" requires reading the CURRENT world,
 * which a snapshot by definition refuses to show. Granularity is the
 * implementor's: a coarse source conflicts on any movement, a
 * per-relation source compares only the footprints' relations, a
 * region-aware source matches delta rows. Conservative refusal is
 * always sound.
 */
public interface Certifiable {

	Pin pin();

	boolean commit(Pin pin, Iterable<Footprint> reads, List<Fact> flush);
}
