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
import com.tgac.logic.lattice.Update;
import com.tgac.logic.lattice.Verdict;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.JoinMap;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerProducer;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Value;

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
	private final Relation rel;

	protected TableParkingPropagator(Relation rel, AnswerProducer producer, Array<? extends Term<?>> watchedTerms) {
		super(watchedTerms);
		this.producer = producer;
		this.rel = rel;
	}

	/** One live disjunct: the row's image, its cells in Term vocabulary, its ⊕-folded condition. */
	@Value
	private static class Row {
		Reified<?> image;
		IndexedSeq<Term<Object>> cells;
		Condition condition;
	}

	@Override
	public Fiber<Verdict> propagate(Package pkg) {
		Array<Term<?>> walked = watchedTerms().map(t -> (Term<?>) pkg.walk(t));
		return probe(pkg, walked)
				.flatMap(this::extension)
				.map(extension -> verdict(pkg, walked, extension));
	}

	/**
	 * The probe as the call key, minted at the one reification site — WITHOUT
	 * the asker's own family. The question is "a region for (x,y)", and the
	 * posted table IS that question: transcribed into its own probe it would
	 * re-animate inside the producer's body and consume the entry mid-
	 * production (a wait-for cycle through the seal). The supports are no
	 * better a citizen: per-wake solver state in the key would fragment the
	 * memo. FD domains and nogoods on the args are the question's honest
	 * context and stay.
	 */
	private Fiber<Call<Relation>> probe(Package pkg, Array<Term<?>> walked) {
		return Residues.about(pkg, lval(walked.map(Term::getObjectTerm)))
				.map(key -> Call.of(rel, key._1,
						Residues.of(key._2.getTheories().remove(TableConstraints.class))));
	}

	/**
	 * Produce drained TO THE SEAL, folded per row by ⊕ — the extension view.
	 * The claimed sub-scope catches forked deliveries: produce's own fiber
	 * can complete while flat-forked deliveries still run, so the seal of
	 * the claiming scope, not produce's return, is what makes the drain
	 * complete.
	 */
	private Fiber<JoinMap<Reified<?>, Condition>> extension(Call<Relation> probe) {
		Scope sub = Scope.scope("TableParkingPropagatorProduction");
		Queue<Tuple2<Reified<?>, Condition>> delivered = new ConcurrentLinkedQueue<>();
		return Fiber.claim(sub, producer.produce(probe, answer -> {
					delivered.add(answer);
					return Fiber.done(Nothing.nothing());
				}))
				.flatMap(explored -> Fiber.sealed(sub))
				.map(sealed -> {
					JoinMap<Reified<?>, Condition> folded = JoinMap.empty(Condition.RING);
					for (Tuple2<Reified<?>, Condition> answer : delivered) {
						folded = folded.append(answer._1, answer._2).getOrElse(folded);
					}
					return folded;
				});
	}

	private Verdict verdict(Package pkg, Array<Term<?>> walked, JoinMap<Reified<?>, Condition> extension) {
		Theory<TableConstraints> theory = Constraint.in(pkg, TableConstraints.class).get().getTheory();
		List<Row> live = extractRows(walked, extension, theory);
		if (live.isEmpty()) {
			return Verdict.fail();
		}
		if (live.stream().anyMatch(row -> entailed(row, walked))) {
			return Verdict.subsumed();
		}
		if (walked.forAll(w -> w.asVal().isDefined())) {
			return discharge(live, walked);
		}
		if (live.size() == 1) {
			Row only = live.get(0);
			if (isUnconditional(only) && isGround(only)) {
				return Verdict.update((state, theory_) ->
						TableConstraints.collapse(state, cast(theory_), walked,
								Array.ofAll(only.getCells().map(Term::get))));
			}
			return discharge(live, walked);
		}
		// the projection reads every condition as TRUE — its sound side. A
		// value pruned here has no supporting row even conditionally, and a
		// singleton binds because EVERY disjunct agrees on it, whatever its
		// condition holds. Nothing is dropped from the constraint: narrow
		// does not commit, the conditions stay in the extension and take
		// full effect at the committing restates. Supports stay plain value
		// sets — ⊕-annotating them per column would still approximate away
		// the rows' cross-column ⊗ structure, a richer lattice not yet earned
		return Verdict.update((state, theory_) ->
				TableConstraints.narrowPatterns(state, cast(theory_), walked,
						live.stream().map(Row::getCells).collect(Collectors.toList())));
	}

	private static boolean isUnconditional(Row only) {
		return Condition.ONE.equals(only.getCondition());
	}

	private static boolean isGround(Row only) {
		return only.getCells().forAll(cell -> cell.asVal().isDefined());
	}

	/**
	 * An ENTAILED row discharges the constraint whole: its disjunct is 1
	 * under the current state and 1 ⊕ a = 1 — absorption, the disjunctive
	 * store's discharge doctrine in its new home.
	 */
	private static boolean entailed(Row row, Array<Term<?>> walked) {
		return isUnconditional(row)
				&& IntStream.range(0, walked.size())
						.allMatch(position -> imposesNothingAt(row, walked, position));
	}

	/** A ground cell must already match; a free cell's couplings must already agree. */
	private static boolean imposesNothingAt(Row row, Array<Term<?>> walked, int position) {
		Term<Object> cell = row.getCells().get(position);
		return cell.asVal().isDefined() ?
				alreadyMatches(cell, walked.get(position)) :
				couplingsAlreadyAgree(row, walked, position);
	}

	/** The walked term holds this very value — unifying them would bind nothing. */
	private static boolean alreadyMatches(Term<Object> cell, Term<?> walked) {
		return walked.asVal().isDefined() && cell.get().equals(walked.get());
	}

	/** Every earlier cell holding the SAME any already walks equal — the coupling is spent. */
	private static boolean couplingsAlreadyAgree(Row row, Array<Term<?>> walked, int position) {
		Term<Object> cell = row.getCells().get(position);
		return IntStream.range(0, position)
				.filter(earlier -> row.getCells().get(earlier).equals(cell))
				.allMatch(earlier -> walked.get(earlier).equals(walked.get(position)));
	}

	private static List<Row> extractRows(Array<Term<?>> walked, JoinMap<Reified<?>, Condition> extension, Theory<TableConstraints> theory) {
		return extension.order.toJavaStream()
				.flatMap(image -> Stream.of(Answers.positions(image))
						.filter(cells -> compatible(theory, walked, cells))
						.map(cells -> new Row(image, cells, extension.members.get(image).get())))
				.collect(Collectors.toList());
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
			return probe(st, walked)
					.flatMap(this::extension)
					.flatMap(extension -> {
						Theory<TableConstraints> theory =
								Constraint.in(st, TableConstraints.class).get().getTheory();
						List<Row> live = extractRows(walked, extension, theory);
						Unifiable<?> anchor = lval(walked.map(Term::getObjectTerm));
						return live.stream()
								.flatMap(row -> row.getCondition().conjuncts().toJavaStream()
										.map(conjunct -> (Goal) Conjunction.of(
												Residues.restate(row.getImage(), conjunct, anchor),
												Propagation.activate(this))))
								.reduce(Goal::or)
								.orElseGet(Goal::failure)
								.apply(st).apply(k);
					});
		};
	}

	/**
	 * The committing verdict: the constraint dissolves into the ⊕ of its live
	 * disjuncts, imposed — one branch per (entry, conjunct), each the same
	 * restate delivery uses, spliced into the run lane after quiescence.
	 */
	private Verdict discharge(List<Row> live, Array<Term<?>> walked) {
		Unifiable<?> anchor = lval(walked.map(Term::getObjectTerm));
		Goal body = live.stream()
				.flatMap(row -> row.getCondition().conjuncts().toJavaStream()
						.map(conjunct -> Residues.restate(row.getImage(), conjunct, anchor)))
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
		return Verdict.update((state, theory_) ->
				Update.applied(cast(theory_).without(this)).withRun(body));
	}

	/**
	 * Unification compatibility with the walked tuple: a free cell admits
	 * everything (couplings are enforced by the restate at commit, not
	 * here — keeping a falsely compatible row only under-filters), a ground
	 * cell against a free column must survive that column's live support.
	 */
	private static boolean compatible(Theory<TableConstraints> theory, Array<Term<?>> walked,
			IndexedSeq<Term<Object>> cells) {
		for (int i = 0; i < walked.size(); i++) {
			Term<?> w = walked.get(i);
			Term<Object> cell = cells.get(i);
			if (!cell.asVal().isDefined()) {
				continue;
			}
			if (w.asVal().isDefined()) {
				if (!cell.get().equals(w.get())) {
					return false;
				}
				continue;
			}
			boolean excluded = TableConstraints.empty().getValue(theory, w)
					.map(support -> !support.admits(cell.get()))
					.getOrElse(false);
			if (excluded) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static Theory<TableConstraints> cast(Theory<?> theory) {
		return (Theory<TableConstraints>) theory;
	}

	/**
	 * The record's rank under the current bindings: the producer's sealed
	 * knowledge where it prices, the optimizer barrier otherwise. Pricing
	 * carries no region: the upper bound stays sound ignoring it.
	 */
	long estimate(Array<Term<?>> walked) {
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
		return new TableParkingPropagator(rel, producer, terms);
	}

	@Override
	public Class<? extends TableConstraints> getFactorClass() {
		return TableConstraints.class;
	}

	@Override
	public String name() {
		return rel.getName() + "@" + producer.id();
	}

	@Override
	public TableConstraints empty() {
		return TableConstraints.empty();
	}
}
