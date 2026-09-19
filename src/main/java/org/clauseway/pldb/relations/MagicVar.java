package org.clauseway.pldb.relations;

// ABOUTME: The marker-variable base: a fresh lvar in every respect except to the
// ABOUTME: ONE consumer that recognizes the wrapper and reads its payload.

import org.clauseway.logic.unification.LVar;
import org.clauseway.logic.unification.Name;
import org.clauseway.logic.unification.Unifiable;
import io.vavr.control.Option;

/**
 * A marker riding a typed argument slot: every term door delegates to a
 * fresh variable, so anywhere but its consumer the marker IS an
 * ordinary lvar — passed into a body it joins, unifies, and projects
 * like any free — and exactly one consumer instanceof-checks the
 * wrapper to read what it carries ({@link Projected}'s claim, a codec,
 * an index declaration). The defining method's signature type-checks
 * the association for free: a marker is a {@code Unifiable<T>}, so it
 * cannot land in a differently-typed slot. Fresh per mint: two markers
 * never couple.
 */
public abstract class MagicVar<T> implements Unifiable<T> {

	private final Unifiable<T> variable = LVar.lvar();

	@Override
	public final Option<LVar<T>> asVar() {
		return variable.asVar();
	}

	@Override
	public final Option<Name<T>> asName() {
		return variable.asName();
	}

	@Override
	public final String toString() {
		return variable.toString();
	}
}
