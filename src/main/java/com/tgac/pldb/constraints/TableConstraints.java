package com.tgac.pldb.constraints;

// ABOUTME: pldb rows as a narrowing constraint store: a posted lookup is a named
// ABOUTME: propagator re-narrowing column supports through the index; branch at labelo.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.Propagation;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.lattice.LatticeStore;
import com.tgac.logic.lattice.Propagator;
import com.tgac.logic.lattice.Update;
import com.tgac.logic.lattice.Verdict;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.Prefix;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import io.vavr.collection.HashSet;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * The table constraint (docs/design/table-constraints.md): a posted lookup is
 * a DOMAIN over candidate rows, not an enumeration. The record is a named
 * value-equal {@link Propagator} watching the argument terms; on every wake it
 * re-queries the index under the current bindings, filters by the live column
 * supports (two posted tables sharing a variable prune each other — the GAC
 * join move), and answers the standard kernel verdicts: no candidates fails,
 * all-ground is a membership check, one candidate COLLAPSES to inferred
 * bindings, many candidates narrow each free column's {@link Support}.
 * Branching happens only at {@link #labelo} — or at reify, where
 * {@code enforce} grounds every surviving support (the FD convention).
 */
public final class TableConstraints extends LatticeStore<Support, TableConstraints> {

	private static final TableConstraints EMPTY =
			new TableConstraints(LinkedHashMap.empty(), HashSet.empty());

	private static final TableConstraints BOTTOM =
			new TableConstraints(LinkedHashMap.empty(), HashSet.empty());

	private TableConstraints(LinkedHashMap<Term<?>, Support> values, HashSet<Propagator> propagators) {
		super(values, propagators);
	}

	public static TableConstraints empty() {
		return EMPTY;
	}

	@Override
	protected TableConstraints create(LinkedHashMap<Term<?>, Support> values, HashSet<Propagator> propagators) {
		return new TableConstraints(values, propagators);
	}

	@Override
	protected TableConstraints bottomStore() {
		return BOTTOM;
	}

	/**
	 * Post a lookup as a constraint: park the table propagator and take its
	 * first examination — the initial narrowing — through the kernel's
	 * statement entry. {@code exists} stays the enumerate-now alternative.
	 */
	public static Goal posted(Database db, Relation rel, Array<Unifiable<?>> args) {
		return p -> Propagation.activate(
						Propagator.of(TableConstraints.class,
								rel.getName() + "@" + Integer.toHexString(System.identityHashCode(db)),
								args,
								body(db, rel)))
				.apply(registered(p));
	}

	/**
	 * The declared branch point: enumerate each variable's LIVE support, in
	 * the given order — collapses cascade between labellings, so later
	 * variables usually bind without branching.
	 */
	public static Goal labelo(Unifiable<?>... xs) {
		return Arrays.stream(xs)
				.map(x -> label(x))
				.reduce(Goal::and)
				.orElseGet(Goal::success);
	}

	/** Answers may not leave with live supports: ground them all, FD-style. */
	@Override
	public <T> Goal enforce(Term<T> x) {
		return values.keySet()
				.map(k -> label(k))
				.foldLeft(Goal.success(), Goal::and);
	}

	private static Goal label(Term<?> x) {
		return s -> {
			Term<?> w = s.walk(x);
			if (!w.asVar().isDefined()) {
				return Cont.just(s);
			}
			return s.getStores().get(TableConstraints.class)
					.map(TableConstraints.class::cast)
					.flatMap(live -> live.getValue(w))
					.map(support -> support.getValues().toJavaStream()
							.map(v -> unifyWith(w, v))
							.reduce(Goal::or)
							.orElseGet(Goal::failure)
							.apply(s))
					.getOrElse(() -> Cont.just(s));
		};
	}

	@SuppressWarnings("unchecked")
	private static Goal unifyWith(Term<?> w, Object value) {
		return ((LVar<Object>) w.asVar().get()).unifies(value);
	}

	private static Package registered(Package p) {
		return p.getStores().containsKey(TableConstraints.class) ? p : p.withStore(EMPTY);
	}

	/**
	 * The record's re-examination, POSITIONAL over the watched terms: walk,
	 * probe the index by whatever is bound, filter by the live supports of
	 * whatever is free, verdict.
	 */
	private static BiFunction<Array<? extends Term<?>>, Package, Verdict> body(Database db, Relation rel) {
		return (watched, pkg) -> {
			Array<Term<?>> walked = watched.map(t -> (Term<?>) pkg.walk(t));
			TableConstraints store = pkg.getStore(TableConstraints.class);
			IndexedSeq<Optional<Object>> probe = walked
					.map(w -> w.asVal()
							.map(v -> (Object) v)
							.toJavaOptional());
			List<Fact> candidates = new ArrayList<>();
			for (Fact fact : db.get(rel, probe)) {
				if (admitted(store, walked, fact)) {
					candidates.add(fact);
				}
			}
			if (candidates.isEmpty()) {
				return Verdict.fail();
			}
			if (walked.forAll(w -> w.asVal().isDefined())) {
				return Verdict.subsumed();
			}
			if (candidates.size() == 1) {
				Fact row = candidates.get(0);
				return Verdict.update((state, factor) ->
						collapse(state, (TableConstraints) factor, walked, row));
			}
			return Verdict.update((state, factor) ->
					narrow(state, (TableConstraints) factor, walked, candidates));
		};
	}

	/** Does the row survive every free column's live support? */
	private static boolean admitted(TableConstraints store, Array<Term<?>> walked, Fact fact) {
		for (int i = 0; i < walked.size(); i++) {
			Term<?> w = walked.get(i);
			if (w.asVal().isDefined()) {
				continue;
			}
			Object cell = fact.getValues().get(i);
			boolean excluded = store.getValue(w)
					.map(support -> !support.admits(cell))
					.getOrElse(false);
			if (excluded) {
				return false;
			}
		}
		return true;
	}

	/** One candidate left: bind every free column — the FD-collapse move on tuples. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Update collapse(Package state, TableConstraints factor, Array<Term<?>> walked, Fact row) {
		Update.Applied result = Update.applied(factor);
		boolean bound = false;
		for (int i = 0; i < walked.size(); i++) {
			Term<?> w = walked.get(i);
			if (w.asVal().isDefined()) {
				continue;
			}
			Prefix prefix = Prefix.binding(state.substitution(),
							(LVar) w.asVar().get(), lval(row.getValues().get(i)))
					.getOrNull();
			if (prefix != null) {
				result = result.withInferred(prefix);
				bound = true;
			}
		}
		return bound ? result : Update.unchanged();
	}

	/** Narrow each free column's support to its projection over the candidates. */
	private static Update narrow(Package state, TableConstraints factor,
			Array<Term<?>> walked, List<Fact> candidates) {
		TableConstraints current = factor;
		List<Prefix> inferred = new ArrayList<>();
		List<Term<?>> reexamine = new ArrayList<>();
		for (int i = 0; i < walked.size(); i++) {
			Term<?> w = walked.get(i);
			if (w.asVal().isDefined()) {
				continue;
			}
			int column = i;
			Support projection = Support.ofAll(candidates.stream()
					.map(f -> f.getValues().get(column))
					.collect(Collectors.toSet()));
			Update step = current.update(state, w, projection);
			TableConstraints before = current;
			current = step.match(
					() -> null,
					() -> before,
					applied -> {
						inferred.addAll(applied.inferred());
						reexamine.addAll(applied.reexamine());
						return (TableConstraints) applied.factor();
					});
			if (current == null) {
				return Update.fail();
			}
		}
		if (current == factor && inferred.isEmpty() && reexamine.isEmpty()) {
			return Update.unchanged();
		}
		Update.Applied result = Update.applied(current);
		for (Prefix prefix : inferred) {
			result = result.withInferred(prefix);
		}
		for (Term<?> x : reexamine) {
			result = result.withReexamine(x);
		}
		return result;
	}

}
