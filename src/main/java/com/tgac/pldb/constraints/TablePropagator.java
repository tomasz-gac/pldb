package com.tgac.pldb.constraints;

// ABOUTME: A posted table as a propagator schema: re-narrowing through the index
// ABOUTME: on wake, and the row enumerator enforce uses to ground survivors.

import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.store.Constraint;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.lattice.Propagator;
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

/**
 * The record's re-examination, POSITIONAL over the watched terms: walk, probe
 * the index by whatever is bound, filter by the live supports of whatever is
 * free, verdict. Also the record's ROW ENUMERATOR: the owning store recognizes
 * this schema on its own propagators and grounds a surviving record at reify
 * by branching over its live candidate rows — {@code posted} is
 * self-sufficient whether or not anything joins it. The name carries the
 * relation and its database, so two posts of one lookup on the same terms are
 * the same knowledge stated twice.
 */
final class TablePropagator extends Propagator<TableConstraints> {

	private final Database db;
	private final Relation rel;

	TablePropagator(Database db, Relation rel, Array<? extends Term<?>> args) {
		super(args);
		this.db = db;
		this.rel = rel;
	}

	@Override
	public Verdict propagate(Package pkg) {
		Array<Term<?>> walked = watchedTerms().map(t -> (Term<?>) pkg.walk(t));
		List<Fact> candidates = candidates(Constraint.in(pkg, TableConstraints.class).get().getFactor(), walked);
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

	@Override
	public TablePropagator watching(Array<? extends Term<?>> terms) {
		return new TablePropagator(db, rel, terms);
	}

	@Override
	public TableConstraints empty() {
		return TableConstraints.empty();
	}

	@Override
	public String name() {
		return rel.getName() + "@" + Integer.toHexString(System.identityHashCode(db));
	}

	@Override
	public Class<? extends TableConstraints> getFactorClass() {
		return TableConstraints.class;
	}

	/**
	 * A post whose bound pattern hits an empty bucket can never be satisfied —
	 * candidates only shrink, so the failure hoists. Monotone under binding
	 * growth: bindings only sharpen the probe.
	 */
	@Override
	public boolean doomed(Package p) {
		return estimate(watchedTerms().map(t -> (Term<?>) p.substitution().walk(t))) == 0;
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
			return candidates(Constraint.in(s, TableConstraints.class).get().getFactor(), walked).stream()
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

	/**
	 * The record's rank for fail-first ordering: the index bucket size under
	 * the current bindings. An upper bound (support filtering not applied) —
	 * a heuristic owes a rank, not exactness, and it costs a bucket lookup
	 * instead of a materialization.
	 */
	long estimate(Array<Term<?>> walked) {
		return db.estimate(rel, probe(walked));
	}

	/** The index probe under the current bindings, filtered by live supports. */
	private List<Fact> candidates(TableConstraints store, Array<Term<?>> walked) {
		IndexedSeq<Optional<Object>> probe = probe(walked);
		List<Fact> candidates = new ArrayList<>();
		for (Fact fact : db.get(rel, probe)) {
			if (admitted(store, walked, fact)) {
				candidates.add(fact);
			}
		}
		return candidates;
	}

	private static IndexedSeq<Optional<Object>> probe(Array<Term<?>> walked) {
		return walked.map(w -> w.asVal()
				.map(v -> (Object) v)
				.toJavaOptional());
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
