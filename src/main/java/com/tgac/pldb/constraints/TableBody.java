package com.tgac.pldb.constraints;

// ABOUTME: A posted table's body: the verdict function over the watched terms, and
// ABOUTME: the row enumerator enforce uses to ground surviving records at reify.

import com.tgac.functional.monad.Cont;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.lattice.Verdict;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.Database;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;

/**
 * The record's re-examination, POSITIONAL over the watched terms: walk, probe
 * the index by whatever is bound, filter by the live supports of whatever is
 * free, verdict. Also the record's ROW ENUMERATOR: the owning store recognizes
 * this body on its own propagators ({@code Propagator.body()}) and grounds a
 * surviving record at reify by branching over its live candidate rows —
 * {@code posted} is self-sufficient whether or not anything joins it.
 */
@RequiredArgsConstructor
final class TableBody implements BiFunction<Array<? extends Term<?>>, Package, Verdict> {

	private final Database db;
	private final Relation rel;

	@Override
	public Verdict apply(Array<? extends Term<?>> watched, Package pkg) {
		Array<Term<?>> walked = watched.map(t -> (Term<?>) pkg.walk(t));
		List<Fact> candidates = candidates(pkg.getStore(TableConstraints.class), walked);
		if (candidates.isEmpty()) {
			return Verdict.fail();
		}
		if (walked.forAll(w -> w.asVal().isDefined())) {
			return Verdict.subsumed();
		}
		if (candidates.size() == 1) {
			Fact row = candidates.get(0);
			return Verdict.update((state, factor) ->
					TableConstraints.collapse(state, (TableConstraints) factor, walked, row));
		}
		return Verdict.update((state, factor) ->
				TableConstraints.narrow(state, (TableConstraints) factor, walked, candidates));
	}

	/**
	 * Branch over the record's LIVE candidate rows, binding every free column —
	 * exactly the rows, never a cartesian product of columns. All-ground is a
	 * no-op: the record verifies itself through the ordinary wake.
	 */
	Goal enumerate(Array<? extends Term<?>> watched) {
		return s -> {
			Array<Term<?>> walked = watched.map(t -> (Term<?>) s.walk(t));
			if (walked.forAll(w -> w.asVal().isDefined())) {
				return Cont.just(s);
			}
			return candidates(s.getStore(TableConstraints.class), walked).stream()
					.map(row -> rowGoal(walked, row))
					.reduce(Goal::or)
					.orElseGet(Goal::failure)
					.apply(s);
		};
	}

	private static Goal rowGoal(Array<Term<?>> walked, Fact row) {
		Goal goal = Goal.success();
		for (int i = 0; i < walked.size(); i++) {
			Term<?> w = walked.get(i);
			if (w.asVal().isDefined()) {
				continue;
			}
			goal = goal.and(unifyWith(w, row.getValues().get(i)));
		}
		return goal;
	}

	@SuppressWarnings("unchecked")
	private static Goal unifyWith(Term<?> w, Object value) {
		return ((LVar<Object>) w.asVar().get()).unifies(value);
	}

	/** The index probe under the current bindings, filtered by live supports. */
	private List<Fact> candidates(TableConstraints store, Array<Term<?>> walked) {
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
		return candidates;
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
}
