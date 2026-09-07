package com.tgac.pldb.constraints;

// ABOUTME: The parking posted table: a Condition-valued constraint over an AnswerProducer —
// ABOUTME: conditional rows impose at commit, Any rows admit everything, verdicts never test.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.fibers.interpreter.Scope;
import com.tgac.logic.constraints.Propagation;
import com.tgac.logic.constraints.store.Constraint;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.goals.Conjunction;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.lattice.ParkingPropagator;
import com.tgac.logic.lattice.Verdict;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.JoinMap;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.tabling.Table;
import com.tgac.logic.tabling.Tabling;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerProducer;
import com.tgac.pldb.GoalProducer;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.control.Either;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The posted table over the ASYNC kind, read as its algebra: the constraint
 * "t ∈ R" over a conditional extension {(rᵢ, Cᵢ)} IS the Condition
 * ⊕ᵢ (t=rᵢ ⊗ Cᵢ), and every wake is a monotone simplification of that
 * value. Rows are patterns — a FREE cell (an Any) admits every value — and
 * conditions are never tested, only IMPOSED at commit: a committing verdict
 * restates the entry whole (image ⊗ condition) through the run lane, so a
 * condition cannot be dropped because it is not separable. Verdicts:
 * <ul>
 * <li>no live entries — fail;</li>
 * <li>some live row ENTAILED (condition {@code ONE}, binding half imposing
 * nothing) — its disjunct is 1 and 1 ⊕ a = 1: subsumed, the alternatives
 * dissolve with the constraint;</li>
 * <li>all watched terms ground — the residual is the ⊕ of the subsuming
 * entries' conditions, discharged as a conde of per-conjunct restates;</li>
 * <li>one live entry — a ground row at {@code ONE} collapses to inferred
 * bindings (the sync kind's move); any other entry discharges by
 * restate;</li>
 * <li>otherwise — narrow the shared free columns, a column any live row
 * leaves free projecting to TOP (absence in the store).</li>
 * </ul>
 *
 * <p>The extension is drained from produce TO THE SEAL before any verdict —
 * the seal is the soundness gate (the extension grows while filtering
 * shrinks) — and folded per row by ⊕ in a {@link JoinMap}, so duplicate
 * derivations of one row factor into a single entry by distributivity.
 */
public class TableParkingPropagator extends ParkingPropagator<TableConstraints> {
	private final Either<AnswerProducer, Tuple2<Goal, Array<Unifiable<?>>>> driver;
	private final Relation rel;

	protected TableParkingPropagator(Relation rel, AnswerProducer producer, Array<? extends Term<?>> watchedTerms) {
		super(watchedTerms);
		this.driver = Either.left(producer);
		this.rel = rel;
	}

	private TableParkingPropagator(Relation rel, Goal rule, Array<Unifiable<?>> heads, Array<? extends Term<?>> watchedTerms) {
		super(watchedTerms);
		this.driver = Either.right(Tuple.of(rule, heads));
		this.rel = rel;
	}

	/**
	 * The rule-backed kind: the extension read from the SOLVE's shared
	 * table. The heads are the variables the body SPEAKS — the production
	 * anchor — carried separately from the watched terms because watching()
	 * re-creations walk the watch list but must never disconnect the body.
	 */
	static TableParkingPropagator rule(Relation rel, Goal rule, Array<Unifiable<?>> heads) {
		return new TableParkingPropagator(rel, rule, heads, heads);
	}

	@Override
	public Fiber<Verdict> propagate(Package pkg) {
		Array<Term<?>> walked = watchedTerms().map(t -> (Term<?>) pkg.walk(t));
		return Extension.probe(pkg, rel, walked)
				.flatMap(probe -> extension(probe, pkg))
				.map(extension -> Extension.verdict(walked,
						Extension.live(walked, extension, theory(pkg)),
						theory -> theory.without(this)));
	}

	private static Theory<TableConstraints> theory(Package pkg) {
		return Constraint.in(pkg, TableConstraints.class).get().getTheory();
	}

	/**
	 * Produce drained TO THE SEAL, folded per row by ⊕ — the extension view.
	 * The claimed sub-scope catches forked deliveries: produce's own fiber
	 * can complete while flat-forked deliveries still run, so the seal of
	 * the claiming scope, not produce's return, is what makes the drain
	 * complete.
	 */
	private Fiber<JoinMap<Reified<?>, Condition>> extension(Call<Relation> probe, Package pkg) {
		Scope sub = Scope.scope("TableParkingPropagatorProduction");
		Queue<Tuple2<Reified<?>, Condition>> delivered = new ConcurrentLinkedQueue<>();
		return Fiber.claim(sub, producerFor(pkg).produce(probe, answer -> {
					delivered.add(answer);
					return Fiber.done(Nothing.nothing());
				}))
				.flatMap(explored -> Fiber.sealed(sub))
				.map(sealed -> Extension.fold(delivered));
	}

	/**
	 * The one drain has one producer: the async kind as posted, or the rule
	 * composed with the SOLVE's table at wake — the residence is the only
	 * thing a rule was missing to be a producer, and it arrives with the
	 * package.
	 */
	private AnswerProducer producerFor(Package pkg) {
		return driver.fold(
				producer -> producer,
				rule -> GoalProducer.of(rel, rule._1, rule._2,
						pkg.getStores().get(Table.class)
								.map(Table.class::cast)
								.getOrElseThrow(() -> new IllegalStateException(
										"posted rule '" + rel.getName()
												+ "' outside a solve: no table in the package"))));
	}


	/**
	 * The record's reify-time grounding: branch over the live disjuncts —
	 * each branch restates its row whole, then RE-WAKES the record so the
	 * verdict that follows discharges it (all-ground, entailed, or the lone
	 * survivor's commit). The parking leg of enforce's row-wise discipline;
	 * the re-wake matters because a branch whose row binds nothing (a wide
	 * or conditional row) would otherwise leave the record parked at reify.
	 */
	Goal enumerate(Array<? extends Term<?>> watched) {
		return st -> k -> {
			Array<Term<?>> walked = watched.map(t -> (Term<?>) st.walk(t));
			if (walked.forAll(w -> w.asVal().isDefined())) {
				return Goal.success().apply(st).apply(k);
			}
			return Extension.probe(st, rel, walked)
					.flatMap(probe -> extension(probe, st))
					.flatMap(extension ->
							Extension.branchRestates(Extension.live(walked, extension, theory(st)), walked)
									.map(branch -> (Goal) Conjunction.of(branch, Propagation.activate(this)))
									.reduce(Goal::or)
									.orElseGet(Goal::failure)
									.apply(st).apply(k));
		};
	}

	/**
	 * The record's rank under the current bindings: the producer's sealed
	 * knowledge where it prices, the optimizer barrier otherwise. Pricing
	 * carries no region: the upper bound stays sound ignoring it.
	 */
	long estimate(Array<Term<?>> walked) {
		return driver.fold(
				producer -> producer.estimate(Call.of(rel, MiniKanren.reify(Substitutions.empty(),
						lval(walked.map(Term::getObjectTerm)).getObjectTerm()).ground())),
				goal -> Long.MAX_VALUE);
	}

	/**
	 * A bound pattern over an empty sealed bucket can never be satisfied —
	 * candidates only shrink. An unpriced producer answers the barrier and
	 * is never doomed here.
	 */
	@Override
	public boolean doomed(Package p) {
		return estimate(watchedTerms().map(t -> (Term<?>) p.substitution().walk(t))) == 0;
	}

	@Override
	public ParkingPropagator<TableConstraints> watching(Array<? extends Term<?>> terms) {
		return driver.fold(
				producer -> new TableParkingPropagator(rel, producer, terms),
				rule -> new TableParkingPropagator(rel, rule._1, rule._2, terms));
	}

	@Override
	public Class<? extends TableConstraints> getFactorClass() {
		return TableConstraints.class;
	}

	@Override
	public String name() {
		return rel.getName() + "@" + driver.fold(AnswerProducer::id, rule -> "rule");
	}

	@Override
	public TableConstraints empty() {
		return TableConstraints.empty();
	}
}
