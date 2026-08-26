package com.tgac.pldb.sql;

// ABOUTME: The nogood family's WHERE compiler: De Morgan over the registry —
// ABOUTME: each literal negates through its own family, whole disjunctions or nothing.

import com.tgac.logic.constraints.Posting;
import com.tgac.logic.constraints.UnifyGoal;
import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.nogoods.Nogood;
import com.tgac.logic.unification.Term;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A nogood is {@code ¬(c₁) ∧ ¬(c₂) ∧ …}, each conjunct {@code ¬(l₁ ∧ … ∧
 * lₙ)} = {@code ¬l₁ ∨ … ∨ ¬lₙ}. Binding-shaped literals negate directly
 * ({@code <>}); store-shaped literals unwrap to their atoms and negate
 * THROUGH THE REGISTRY — compile positively via the literal's own family,
 * then take the guarded complement, which refuses whenever the family's
 * compilation was a weakening (the inverted direction: a weakening's
 * complement under-delivers). A disjunction pushes WHOLE or not at all —
 * dropping a disjunct strengthens — while the atom's conjunct level may
 * drop freely (weaker), marked {@code weakened} when it does.
 *
 * <p>The registry reference is the live compiler map — the special case
 * Tom ruled for: nogoods are engine-core, so cross-family delegation is
 * wired at construction rather than widening the compiler signature.
 */
public final class NogoodSqlCompiler implements SqlCompiler {

	private final Map<Class<?>, SqlCompiler> families;

	public NogoodSqlCompiler(Map<Class<?>, SqlCompiler> families) {
		this.families = families;
	}

	@Override
	public Optional<SqlPredicate> compile(Atom<?> atom, ColumnResolver columns) {
		if (!(atom instanceof Nogood)) {
			return Optional.empty();
		}
		List<SqlPredicate> conjuncts = new ArrayList<>();
		boolean dropped = false;
		for (Posting conjunct : ((Nogood) atom).getForbidden()) {
			Optional<SqlPredicate> compiled = negatedConjunct(conjunct, columns);
			if (compiled.isPresent()) {
				conjuncts.add(compiled.get());
			} else {
				dropped = true;
			}
		}
		if (conjuncts.isEmpty()) {
			return Optional.empty();
		}
		SqlPredicate all = conjuncts.size() == 1 ?
				conjuncts.get(0) :
				SqlPredicate.and(conjuncts);
		return Optional.of(dropped ? all.weakened() : all);
	}

	/** {@code ¬(l₁ ∧ … ∧ lₙ)} disjoins the negations — whole or not at all. */
	private Optional<SqlPredicate> negatedConjunct(Posting conjunct, ColumnResolver columns) {
		// conjuncts arrive FLAT by the Nogood envelope's own invariant
		// (nested alls are one conjunction, flattened at construction);
		// an AllOf reaching the literal level refuses conservatively below
		List<Posting> literals = conjunct instanceof Posting.AllOf ?
				((Posting.AllOf) conjunct).getParts().toJavaList() :
				Collections.singletonList(conjunct);
		List<SqlPredicate> negations = new ArrayList<>();
		for (Posting literal : literals) {
			Optional<SqlPredicate> negation = negatedLiteral(literal, columns);
			if (!negation.isPresent()) {
				return Optional.empty();
			}
			negations.add(negation.get());
		}
		return Optional.of(negations.size() == 1 ?
				negations.get(0) :
				SqlPredicate.or(negations));
	}

	private Optional<SqlPredicate> negatedLiteral(Posting literal, ColumnResolver columns) {
		return literal.accept(new Posting.Visitor<Optional<SqlPredicate>>() {

			@Override
			public Optional<SqlPredicate> visit(UnifyGoal<?> unification) {
				Optional<String> leftColumn = columns.columnOf(unification.getU());
				Optional<String> rightColumn = columns.columnOf(unification.getV());
				Optional<Object> leftValue = value(unification.getU());
				Optional<Object> rightValue = value(unification.getV());
				if (leftColumn.isPresent() && rightColumn.isPresent()) {
					return Optional.of(SqlPredicate.neqColumns(leftColumn.get(), rightColumn.get()));
				}
				if (leftColumn.isPresent() && rightValue.isPresent()) {
					return Optional.of(SqlPredicate.neq(leftColumn.get(), rightValue.get()));
				}
				if (leftValue.isPresent() && rightColumn.isPresent()) {
					return Optional.of(SqlPredicate.neq(rightColumn.get(), leftValue.get()));
				}
				return Optional.empty();
			}

			@Override
			public Optional<SqlPredicate> visit(Posting.Activation activation) {
				Atom<?> item = activation.getItem();
				SqlCompiler family = families.get(item.getFactorClass());
				return family == null ?
						Optional.empty() :
						family.compile(item, columns).flatMap(SqlPredicate::negated);
			}

			@Override
			public Optional<SqlPredicate> visit(Posting.Resolution resolution) {
				// a CROSSING resolution already became its unifications
				// (Renamer: a prefix crosses as the conjunction of its
				// binds), so what arrives here compiled through the region
				// is UnifyGoals; an uncrossed one refuses conservatively
				return Optional.empty();
			}

			@Override
			public Optional<SqlPredicate> visit(Posting.Absorption absorption) {
				return Optional.empty();
			}

			@Override
			public Optional<SqlPredicate> visit(Posting.AllOf all) {
				return Optional.empty();
			}
		});
	}

	private static Optional<Object> value(Term<?> term) {
		return term.asVal()
				.map(v -> (Object) v)
				.toJavaOptional();
	}
}
