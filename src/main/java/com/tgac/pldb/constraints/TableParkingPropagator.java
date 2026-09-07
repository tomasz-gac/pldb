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
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
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

	private final AnswerProducer producer;
	private final Goal rule;
	private final Array<Unifiable<?>> heads;
	private final Relation rel;

	protected TableParkingPropagator(Relation rel, AnswerProducer producer, Array<? extends Term<?>> watchedTerms) {
		this(rel, producer, null, null, watchedTerms);
	}

	private TableParkingPropagator(Relation rel, AnswerProducer producer, Goal rule,
			Array<Unifiable<?>> heads, Array<? extends Term<?>> watchedTerms) {
		super(watchedTerms);
		this.producer = producer;
		this.rule = rule;
		this.heads = heads;
		this.rel = rel;
	}

	/**
	 * The rule-backed kind: the extension read from the SOLVE's shared
	 * table. The heads are the variables the body SPEAKS — the production
	 * anchor — carried separately from the watched terms because watching()
	 * re-creations walk the watch list but must never disconnect the body.
	 */
	static TableParkingPropagator rule(Relation rel, Goal rule, Array<Unifiable<?>> heads) {
		return new TableParkingPropagator(rel, null, rule, heads, heads);
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
		Fiber<Nothing> drained = rule != null
				? ruleDeliveries(probe, pkg, delivered)
				: producer.produce(probe, answer -> {
					delivered.add(answer);
					return Fiber.done(Nothing.nothing());
				});
		return Fiber.claim(sub, drained)
				.flatMap(explored -> Fiber.sealed(sub))
				.map(sealed -> Extension.fold(delivered));
	}

	/**
	 * The rule's extension from the SOLVE's shared table: the tabled call is
	 * consumed to exhaustion on a clean package carrying the solve's
	 * {@link Table}, so the entry this drain produces (or awaits) is the
	 * same one every goal-side consumer of the relation reads — one
	 * production serves both drivers.
	 */
	private Fiber<Nothing> ruleDeliveries(Call<Relation> probe, Package pkg,
			Queue<Tuple2<Reified<?>, Condition>> delivered) {
		Table table = pkg.getStores().get(Table.class)
				.map(Table.class::cast)
				.getOrElseThrow(() -> new IllegalStateException(
						"posted rule '" + rel.getName() + "' outside a solve: no table in the package"));
		// the anchor IS the captured heads: the probe image restates onto the
		// variables the body speaks, the tabled call keys off their resulting
		// bindings (alpha-aligned with goal-side consumption at the same
		// probe), and deliveries image back over them — the capture pattern,
		// sound on the clean package because its lineage is disjoint
		Unifiable<Object> anchor = lval(heads.map(Unifiable::getObjectUnifiable));
		Goal seeded = Conjunction.of(
				Residues.restate(probe.getArguments(), probe.getResidues(), anchor),
				Tabling.call(rel, heads.map(Unifiable::getObjectUnifiable), () -> rule));
		return seeded.apply(Package.empty().withStore(table)).apply(answerPkg ->
				Residues.all(answerPkg, anchor).flatMap(answer -> {
					delivered.add(Tuple.of(answer._1, Condition.of(answer._2)));
					return Fiber.done(Nothing.nothing());
				}));
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
		if (rule != null) {
			return Long.MAX_VALUE;
		}
		Reified<?> image = MiniKanren.reify(Substitutions.empty(),
						lval(walked.map(Term::getObjectTerm)).getObjectTerm())
				.ground();
		return producer.estimate(Call.of(rel, image));
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
		return new TableParkingPropagator(rel, producer, rule, heads, terms);
	}

	@Override
	public Class<? extends TableConstraints> getFactorClass() {
		return TableConstraints.class;
	}

	@Override
	public String name() {
		return rel.getName() + "@" + (producer != null ? producer.id() : "rule");
	}

	@Override
	public TableConstraints empty() {
		return TableConstraints.empty();
	}
}
