package com.tgac.pldb.sql;

// ABOUTME: The FD family's WHERE compiler: domain impositions become in/between/eq,
// ABOUTME: leq and separate propagators become comparisons — by name, the sanctioned identity.

import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.finitedomain.Domain;
import com.tgac.logic.finitedomain.domains.DomainVisitor;
import com.tgac.logic.finitedomain.domains.Empty;
import com.tgac.logic.finitedomain.domains.EnumeratedDomain;
import com.tgac.logic.finitedomain.domains.Interval;
import com.tgac.logic.finitedomain.domains.Singleton;
import com.tgac.logic.finitedomain.domains.Union;
import com.tgac.logic.lattice.Imposition;
import com.tgac.logic.lattice.Propagator;
import com.tgac.logic.unification.Term;
import io.vavr.collection.Array;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registered under the FD family class. Domain impositions compile through
 * the domain's own visitor — enumeration to {@code in}, interval to
 * {@code between} (both bounds inclusive on both sides), point to
 * {@code eq}; {@code Empty} and {@code Union} return nothing (the AST
 * ruling defers disjunctive structure, and an empty domain is a dying
 * branch propagation will kill). Order propagators are recognized by NAME
 * — a propagator's name uniquely determines its semantics — and compile
 * positionally: {@code leq} and {@code separate} over two operands, each
 * a column ({@code _.i}) or a ground value.
 */
public final class FiniteDomainSqlCompiler implements SqlCompiler {

	@Override
	public Optional<SqlPredicate> compile(Atom<?> atom, ColumnResolver columns) {
		if (atom instanceof Imposition) {
			return imposition((Imposition<?, ?>) atom, columns);
		}
		if (atom instanceof Propagator && ((Propagator<?>) atom).watchedTerms().size() == 2) {
			return comparison(atom, ((Propagator<?>) atom).watchedTerms(), columns);
		}
		return Optional.empty();
	}

	private static Optional<SqlPredicate> imposition(Imposition<?, ?> atom, ColumnResolver columns) {
		Optional<String> column = columns.columnOf(atom.getTarget());
		if (!column.isPresent() || !(atom.getValue() instanceof Domain)) {
			return Optional.empty();
		}
		return domain(column.get(), (Domain<?>) atom.getValue());
	}

	@SuppressWarnings("unchecked")
	private static Optional<SqlPredicate> domain(String column, Domain<?> value) {
		return ((Domain<Object>) value).accept(new DomainVisitor<Object, Optional<SqlPredicate>>() {
			@Override
			public Optional<SqlPredicate> visit(Empty<Object> domain) {
				return Optional.empty();
			}

			@Override
			public Optional<SqlPredicate> visit(Singleton<Object> domain) {
				return Optional.of(SqlPredicate.eq(column, domain.getValue().getValue()));
			}

			@Override
			public Optional<SqlPredicate> visit(Interval<Object> domain) {
				return Optional.of(SqlPredicate.between(column,
						domain.getMin().getValue(), domain.getMax().getValue()));
			}

			@Override
			public Optional<SqlPredicate> visit(Union<Object> domain) {
				// the hull: BETWEEN min AND max selects a SUPERSET (the holes
				// come back, filtered locally) — weaker than the atom, the
				// lawful direction; exact disjunctive structure waits on the
				// deferred AST ruling
				return Optional.of(SqlPredicate.between(column,
						domain.min().getValue(), domain.max().getValue()));
			}

			@Override
			public Optional<SqlPredicate> visit(EnumeratedDomain<Object> domain) {
				return Optional.of(SqlPredicate.in(column,
						domain.stream().collect(Collectors.toList())));
			}
		});
	}

	private static Optional<SqlPredicate> comparison(Atom<?> atom,
			Array<? extends Term<?>> operands, ColumnResolver columns) {
		Optional<String> left = columns.columnOf(operands.get(0));
		Optional<String> right = columns.columnOf(operands.get(1));
		Optional<Object> leftValue = value(operands.get(0));
		Optional<Object> rightValue = value(operands.get(1));

		if ("leq".equals(atom.name())) {
			if (left.isPresent() && right.isPresent()) {
				return Optional.of(SqlPredicate.leqColumns(left.get(), right.get()));
			}
			if (left.isPresent() && rightValue.isPresent()) {
				return Optional.of(SqlPredicate.leq(left.get(), rightValue.get()));
			}
			if (leftValue.isPresent() && right.isPresent()) {
				// left <= right with the column on the RIGHT: the column-first
				// spelling flips the operator — 5 <= col IS col >= 5
				return Optional.of(SqlPredicate.geq(right.get(), leftValue.get()));
			}
		}
		if ("separate".equals(atom.name())) {
			if (left.isPresent() && right.isPresent()) {
				return Optional.of(SqlPredicate.neqColumns(left.get(), right.get()));
			}
			if (left.isPresent() && rightValue.isPresent()) {
				return Optional.of(SqlPredicate.neq(left.get(), rightValue.get()));
			}
			if (leftValue.isPresent() && right.isPresent()) {
				return Optional.of(SqlPredicate.neq(right.get(), leftValue.get()));
			}
		}
		return Optional.empty();
	}

	private static Optional<Object> value(Term<?> term) {
		return term.asVal()
				.map(v -> (Object) v)
				.toJavaOptional();
	}
}
