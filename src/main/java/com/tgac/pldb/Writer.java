package com.tgac.pldb;

// ABOUTME: The write face: facts stated as literals, the door converts — the
// ABOUTME: one interface every appendable value (Database, Transaction) wears.

import com.tgac.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.Collection;

/**
 * A value that accepts appends stated in the same language reads use:
 * LITERALS, ground. The door converts to rows and a hole refuses by
 * relation and column; {@code Fact} stays beneath the doors as the
 * internal row carrier ({@code Collection<Literal>} and
 * {@code List<Fact>} erase differently, so both doors share a name).
 */
public interface Writer<S extends Writer<S>> {

	Try<S> asserting(Collection<Literal> rows);

	default Try<S> asserting(Literal... rows) {
		return asserting(Arrays.asList(rows));
	}
}
