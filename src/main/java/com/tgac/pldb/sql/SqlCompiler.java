package com.tgac.pldb.sql;

// ABOUTME: The adapter-side compiler a user registers per constraint family:
// ABOUTME: one atom in, optionally one WHERE predicate out — weaker or equal.

import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.unification.Term;
import java.util.Optional;

/**
 * Compiles one constraint atom into the adapter's predicate vocabulary.
 * Registered on a source per FACTOR CLASS, so user-created constraint
 * families push down without the adapter enumerating them; an unregistered
 * family simply stays local — silence, not error.
 *
 * <p>THE ONE LAW: the compiled predicate must be entailed by the atom —
 * weaker or equal, never stronger. A predicate that selects fewer rows
 * than the atom admits makes the source under-deliver, and lost answers
 * are silent. Returning {@link Optional#empty()} for anything uncertain
 * is always correct: the atom's narrowing stays local, enforced by
 * propagation over the returned rows.
 *
 * <p>Atoms arrive with their watched terms renamed to POSITIONAL names:
 * the resolver answers {@code _.i} with the i-th column, and ground terms
 * with nothing — a compiler reads its operands through it and never sees
 * tables.
 */
public interface SqlCompiler {

	Optional<SqlPredicate> compile(Atom<?> atom, ColumnResolver columns);

	interface ColumnResolver {
		Optional<String> columnOf(Term<?> term);
	}
}
