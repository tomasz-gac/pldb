package com.tgac.pldb.relations;

// ABOUTME: The projection marker: an ∃-projected column stated inline — a real
// ABOUTME: fresh variable everywhere except the builder, which sees the wrapper.

import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.Name;
import com.tgac.logic.unification.Unifiable;
import io.vavr.control.Option;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A column the caller ∃-projects away, stated at the call site. The
 * marker IS a fresh variable — every term door delegates to one, so
 * inside a rule body it joins like any lvar — and only the builder
 * reads the wrapper type: a marked column drops out of the literal's
 * head into a generated projection rule, whose tabled cell folds
 * duplicates (set semantics) and whose posted form makes
 * {@code exclude} an honest ¬∃. Fresh per call: two markers never
 * couple.
 */
public final class Projected<T> implements Unifiable<T> {

	private final Unifiable<T> variable = LVar.lvar();
	private final AtomicBoolean claimed = new AtomicBoolean();

	private Projected() {
	}

	/**
	 * One-shot: the FIRST arg-list a marker appears in owns the
	 * projection — and that is always the literal the caller wrote it
	 * into, because a defining method declares its args before its body
	 * builds. Later appearances (the marker forwarded into body
	 * literals) read it as the plain variable it delegates to, so a
	 * marker used as a join variable stays ONE variable.
	 */
	boolean claim() {
		return claimed.compareAndSet(false, true);
	}

	public static <T> Unifiable<T> projected() {
		return new Projected<>();
	}

	@Override
	public Option<LVar<T>> asVar() {
		return variable.asVar();
	}

	@Override
	public Option<Name<T>> asName() {
		return variable.asName();
	}

	@Override
	public String toString() {
		return variable.toString();
	}
}
