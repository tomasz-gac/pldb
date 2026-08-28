package com.tgac.pldb.constraints;

// ABOUTME: A posted table as a propagator schema: re-narrowing through the index
// ABOUTME: on wake, and the row enumerator enforce uses to ground survivors.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.store.Constraint;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.lattice.Propagator;
import com.tgac.logic.lattice.Verdict;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * The record's re-examination, POSITIONAL over the watched terms: walk, probe
 * the source by the call key under the current bindings, filter by the live
 * supports of whatever is free, verdict. Also the record's ROW ENUMERATOR: the
 * owning store recognizes this schema on its own propagators and grounds a
 * surviving record at reify by branching over its live candidate rows —
 * {@code posted} is self-sufficient whether or not anything joins it. The name
 * carries the relation and its source, so two posts of one lookup on the same
 * terms are the same knowledge stated twice.
 */
final class TablePropagator extends Propagator<TableConstraints> {

	private final AnswerSource source;
	private final Relation rel;

	TablePropagator(AnswerSource source, Relation rel, Array<? extends Term<?>> args) {
		super(args);
		this.source = source;
		this.rel = rel;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Verdict propagate(Package pkg) {
		Array<Term<?>> walked = watchedTerms().map(t -> (Term<?>) pkg.walk(t));
		List<Array<Object>> candidates =
				candidates(pkg, Constraint.in(pkg, TableConstraints.class).get().getTheory(), walked);
		if (candidates.isEmpty()) {
			return Verdict.fail();
		}
		if (walked.forAll(w -> w.asVal().isDefined())) {
			return Verdict.subsumed();
		}
		if (candidates.size() == 1) {
			Array<Object> row = candidates.get(0);
			return Verdict.update((state, theory) ->
					TableConstraints.collapse(state, (Theory<TableConstraints>) theory, walked, row));
		}
		return Verdict.update((state, theory) ->
				TableConstraints.narrow(state, (Theory<TableConstraints>) theory, walked, candidates));
	}

	@Override
	public TablePropagator watching(Array<? extends Term<?>> terms) {
		return new TablePropagator(source, rel, terms);
	}

	@Override
	public TableConstraints empty() {
		return TableConstraints.empty();
	}

	@Override
	public String name() {
		return rel.getName() + "@" + source.id();
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
			return candidates(s, Constraint.in(s, TableConstraints.class).get().getTheory(), walked).stream()
					.map(row -> rowGoal(walked, row))
					.reduce(Goal::or)
					.orElseGet(Goal::failure)
					.apply(s);
		};
	}

	private static Goal rowGoal(Array<Term<?>> walked, Array<Object> row) {
		Goal goal = Goal.success();
		for (int i = 0; i < walked.size(); i++) {
			Term<?> w = walked.get(i);
			if (w.asVal().isDefined()) {
				continue;
			}
			goal = goal.and(unifyWith(w, row.get(i)));
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
	 * instead of a materialization. Pricing carries no region: the upper
	 * bound stays sound ignoring it.
	 */
	long estimate(Array<Term<?>> walked) {
		Reified<?> image = MiniKanren.reify(Substitutions.empty(),
						lval(walked.map(Term::getObjectTerm)).getObjectTerm())
				.ground();
		return source.estimate(Call.of(rel, image));
	}

	/** The source probe under the current bindings, filtered by live supports. */
	private List<Array<Object>> candidates(Package pkg, Theory<TableConstraints> theory,
			Array<Term<?>> walked) {
		Call<Relation> probe = probe(pkg, walked);
		List<Array<Object>> candidates = new ArrayList<>();
		for (Tuple2<Reified<?>, Condition> answer : source.answers(probe)) {
			if (!Condition.ONE.equals(answer._2)) {
				// GAC over conditional candidates is open research: skipping
				// would under-deliver (a wrongly failing record), so refuse
				throw new IllegalStateException(
						"conditional answers are not yet consumed by the posted table: " + answer);
			}
			Array<Object> row = Answers.values(answer._1);
			if (admitted(theory, walked, row)) {
				candidates.add(row);
			}
		}
		return candidates;
	}

	/** The probe as the call key, minted at the one reification site. */
	private Call<Relation> probe(Package pkg, Array<Term<?>> walked) {
		Tuple2<Reified<?>, Residues> key = Residues.about(pkg,
						lval(walked.map(Term::getObjectTerm)))
				.ground();
		return Call.of(rel, key._1, key._2);
	}

	/** Does the row survive every free column's live support? */
	private static boolean admitted(Theory<TableConstraints> theory, Array<Term<?>> walked,
			Array<Object> row) {
		for (int i = 0; i < walked.size(); i++) {
			Term<?> w = walked.get(i);
			if (w.asVal().isDefined()) {
				continue;
			}
			Object cell = row.get(i);
			boolean excluded = TableConstraints.empty().getValue(theory, w)
					.map(support -> !support.admits(cell))
					.getOrElse(false);
			if (excluded) {
				return false;
			}
		}
		return true;
	}
}
