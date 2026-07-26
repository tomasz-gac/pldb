package com.tgac.pldb.constraints;

// ABOUTME: pldb rows as a narrowing constraint store: a posted lookup is a named
// ABOUTME: propagator re-narrowing column supports through the index; branch at labelo.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.Propagation;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.goals.optimizer.Bounded;
import com.tgac.logic.lattice.LatticeStore;
import com.tgac.logic.lattice.Propagator;
import com.tgac.logic.lattice.Update;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.Prefix;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import io.vavr.collection.HashSet;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.LinkedHashMap;
import io.vavr.control.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
 * {@code enforce} grounds each surviving record row-wise.
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
	 * Priced 0-or-1: a post whose bound pattern hits an empty bucket can
	 * never be satisfied (candidates only shrink), so the planner hoists the
	 * failure; a live post is a constraint statement — one success, ever —
	 * so the planner floats it ahead of enumerations.
	 */
	public static Goal posted(Database db, Relation rel, Array<Unifiable<?>> args) {
		Goal post = p -> Propagation.activate(
						Propagator.of(TableConstraints.class,
								rel.getName() + "@" + Integer.toHexString(System.identityHashCode(db)),
								args,
								new TableBody(db, rel)))
				.apply(registered(p));
		return Bounded.of(s -> postedOrder(s, db, rel, args), post);
	}

	private static long postedOrder(Substitutions s, Database db, Relation rel, Array<Unifiable<?>> args) {
		IndexedSeq<Optional<Object>> probe = args
				.map(u -> (Unifiable<?>) s.walk(u))
				.map(Unifiable::getObjectUnifiable)
				.map(Unifiable::asVal)
				.map(Option::toJavaOptional);
		return db.estimate(rel, probe) == 0 ? 0 : 1;
	}

	/**
	 * The declared branch point: enumerate each variable's LIVE support, in
	 * the given order — collapses cascade between labellings, so later
	 * variables usually bind without branching. Each labelling is priced at
	 * its live support size, so the optimizer's cheapest-first sort is CP's
	 * min-domain heuristic.
	 */
	public static Goal labelo(Unifiable<?>... xs) {
		return Arrays.stream(xs)
				.map(x -> Bounded.sighted(p -> labelOrder(p, x), label(x)))
				.reduce(Goal::and)
				.orElseGet(Goal::success);
	}

	private static long labelOrder(Package p, Term<?> x) {
		Term<?> w = p.walk(x);
		if (!w.asVar().isDefined()) {
			return 1;
		}
		return p.getStores().get(TableConstraints.class)
				.map(TableConstraints.class::cast)
				.flatMap(live -> live.getValue(w))
				.map(support -> (long) support.getValues().size())
				.getOrElse(1L);
	}

	/**
	 * Answers may not leave with live records: each surviving record grounds
	 * ROW-WISE — the store recognizes its own bodies and branches over each
	 * record's live candidates, FEWEST CANDIDATES FIRST (fail-first: each
	 * grounding collapses the rest, so the narrowest record minimizes total
	 * branching). Self-sufficient for lone and joined records alike.
	 */
	@Override
	public <T> Goal enforce(Term<T> x) {
		return groundRecords();
	}

	/** Pick the narrowest live record, enumerate it, repeat against the new state. */
	private static Goal groundRecords() {
		return s -> {
			TableConstraints live = s.getStores().get(TableConstraints.class)
					.map(TableConstraints.class::cast)
					.getOrNull();
			if (live == null) {
				return Cont.just(s);
			}
			TableBody narrowest = null;
			Array<? extends Term<?>> watched = null;
			int fewest = Integer.MAX_VALUE;
			for (Propagator p : live.propagators) {
				if (!(p.body() instanceof TableBody)) {
					continue;
				}
				Array<Term<?>> walked = p.watchedTerms().map(t -> (Term<?>) s.walk(t));
				if (walked.forAll(w -> w.asVal().isDefined())) {
					continue;
				}
				int count = ((TableBody) p.body()).candidates(live, walked).size();
				if (count < fewest) {
					fewest = count;
					narrowest = (TableBody) p.body();
					watched = p.watchedTerms();
				}
			}
			if (narrowest == null) {
				return Cont.just(s);
			}
			return narrowest.enumerate(watched)
					.and(Goal.defer(TableConstraints::groundRecords))
					.apply(s);
		};
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

	/** One candidate left: bind every free column — the FD-collapse move on tuples. */
	static Update collapse(Package state, TableConstraints factor, Array<Term<?>> walked, Fact row) {
		Update.Applied result = Update.applied(factor);
		boolean bound = false;
		for (int i = 0; i < walked.size(); i++) {
			Term<?> w = walked.get(i);
			if (w.asVal().isDefined()) {
				continue;
			}
			Prefix prefix = bindingOf(state, w, row.getValues().get(i));
			if (prefix != null) {
				result = result.withInferred(prefix);
				bound = true;
			}
		}
		return bound ? result : Update.unchanged();
	}

	private static Prefix bindingOf(Package state, Term<?> w, Object value) {
		return Prefix.binding(state.substitution(), w.asVar().get(), lval(value))
				.getOrNull();
	}

	/**
	 * Is the column a JOIN column right now? Someone already stored a support
	 * for it, or a second propagator watches it — sharedness is checked per
	 * wake, not per post, because it arrives late (a later posting, or an
	 * alias welding two columns).
	 */
	private static boolean shared(TableConstraints store, Package state, Term<?> w) {
		if (store.getValue(w).isDefined()) {
			return true;
		}
		int watchers = 0;
		for (Propagator p : store.propagators) {
			if (p.watches(state, w) && ++watchers >= 2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Narrow each free column against its projection over the candidates.
	 * Projections are TRANSIENT: a singleton binds its column right here
	 * (every candidate agrees, and some candidate must hold), and a wider
	 * projection is STORED only when the column is shared — an unshared
	 * support has no reader, so the shadow's cost is the join width, not
	 * every posted column.
	 */
	static Update narrow(Package state, TableConstraints factor,
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
			if (projection.asPoint().isDefined()) {
				Prefix prefix = bindingOf(state, w, projection.asPoint().get());
				if (prefix != null) {
					inferred.add(prefix);
				}
				continue;
			}
			if (!shared(current, state, w)) {
				continue;
			}
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
