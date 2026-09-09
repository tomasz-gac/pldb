package com.tgac.pldb;

// ABOUTME: The RENTED certify capability: the backend validates read sets at
// ABOUTME: commit itself; the source owns the whole commit door and its dialect.

import com.tgac.pldb.relations.Fact;
import java.util.List;

/**
 * A source whose backend certifies reads itself — a serializable
 * isolation that actually validates read sets (PostgreSQL's SSI, the
 * two-phase-locking implementations). {@link #commit} lands the flush
 * and commits the source's own snapshot transaction; a refusal in the
 * backend's conflict dialect answers {@code false} (the world moved —
 * re-solve), any other failure throws. No pin and no footprint: the
 * backend tracked the reads itself. The implementor is the adapter
 * that KNOWS its backend keeps that promise; a backend whose
 * SERIALIZABLE is snapshot isolation in costume must not wear this
 * interface.
 */
public interface CertifiedReads extends AnswerSource, AutoCloseable {

	boolean commit(List<Fact> flush);
}
