package com.tgac.pldb;

// ABOUTME: The read face of an external relation backend: facts matching a bound
// ABOUTME: pattern, plus the planner's cardinality estimate over the same probe.

import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.IndexedSeq;
import java.util.Optional;

/**
 * A source of facts: what a relation lookup consumes. The bound pattern is
 * the probe — the relation plus one {@code Optional} per argument position,
 * present where the argument is ground. A source answers with every fact
 * matching the pattern; narrowing beyond the pattern (domains, exclusions)
 * stays local, enforced by propagation over the returned rows, so a source
 * may only ever over-deliver — never under-deliver — for the probe it was
 * given. {@link Database} is the in-memory reference; remote backends
 * implement the same face behind a row-to-{@link Fact} mapping.
 */
public interface FactSource {

	Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args);

	/**
	 * The source's identity, as knowledge: two posts of one lookup are the
	 * same constraint exactly when their sources answer for the same data,
	 * and this string is what says so. The default is object identity —
	 * right for in-memory values, where each database IS its data; a
	 * backend reachable through many handles must override with a declared
	 * id, or equal lookups against it will read as distinct knowledge.
	 */
	default String id() {
		return Integer.toHexString(System.identityHashCode(this));
	}

	/**
	 * Upper bound on the facts {@link #get} would yield — the planner's order
	 * function (logic's optimizer.md §3). Exposure, not computation: the
	 * default counts, backends with sized buckets should override.
	 */
	default long estimate(Relation relation, IndexedSeq<Optional<Object>> args) {
		Iterable<Fact> bucket = get(relation, args);
		if (bucket instanceof java.util.Collection) {
			return ((java.util.Collection<?>) bucket).size();
		}
		long n = 0;
		for (@SuppressWarnings("unused") Fact f : bucket) {
			n++;
		}
		return n;
	}
}
