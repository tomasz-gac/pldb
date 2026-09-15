package com.tgac.pldb;

// ABOUTME: The write face: facts stated as literals, the door converts — asserting
// ABOUTME: lands rows, retracting removes them by fact, both refusing holes loudly.

import com.tgac.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.Collection;

/**
 * A value that accepts writes stated in the same language reads use:
 * LITERALS, ground. The doors convert to rows and a hole refuses by
 * relation and column; {@code Fact} stays beneath the doors as the
 * internal row carrier ({@code Collection<Literal>} and
 * {@code List<Fact>} erase differently, so the doors share names).
 * {@link #retracting} removes BY FACT — the membership claim leaves
 * whole, every physical duplicate with it; retracting what is absent
 * is a set-semantics no-op at a store, and a transaction that both
 * asserts and retracts one fact refuses as {@code Conflict}: it has
 * not decided what it believes.
 */
public interface Writer<S extends Writer<S>> {

	Try<S> asserting(Collection<Literal> rows);

	default Try<S> asserting(Literal... rows) {
		return asserting(Arrays.asList(rows));
	}

	Try<S> retracting(Collection<Literal> rows);

	default Try<S> retracting(Literal... rows) {
		return retracting(Arrays.asList(rows));
	}
}
