package com.tgac.pldb;

// ABOUTME: Constructive negation over derived relations: the sealed extension's rows
// ABOUTME: translate to nogoods, and the trial's filter carries conditional rows too.

import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.fibers.schedulers.BreadthFirstScheduler;
import com.tgac.logic.constraints.Posting;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.relations.RelationN;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Any goal compressed into a derived relation can be DENIED: the sealed
 * extension is the completeness certificate (the closed world is the
 * relation's own definition over its pinned sources), and de Morgan over
 * its rows lands in the nogood store's native shape — each row a
 * forbidden conjunction, wide cells dropped (a stronger exclusion),
 * couplings as disequalities. Conditional rows ride as NESTED exclusions:
 * the trial judges the inner literal three ways, so the nogood functions
 * as it always has — a filter over the complement, never positive
 * knowledge — and no clause store is needed for soundness.
 */
public class DerivedNegationTest {

	private static final RelationN r = RelationN.of("r",
			Property.of("item"), Property.of("tag"));

	/** A probe image: bound slots carry their value, nulls are free. */
	private static Call<Relation> probe(Object... slots) {
		List<Object> members = new ArrayList<>();
		int frees = 0;
		for (Object slot : slots) {
			members.add(slot == null ? Any.of(frees++) : lval(slot));
		}
		return Call.of(r, (Reified<?>) lval(Array.ofAll(members)));
	}

	/** Drive produce to completion, collecting the sealed extension. */
	private static List<Tuple2<Reified<?>, Condition>> drain(TabledSource source) {
		List<Tuple2<Reified<?>, Condition>> collected = new ArrayList<>();
		new BreadthFirstScheduler<>(source.produce(probe(null, null), answer -> {
			collected.add(answer);
			return Fiber.done(Nothing.nothing());
		})).get();
		return collected;
	}

	/** The exact answers for {@code out}, rendered and sorted. */
	private static List<String> answers(Goal goal, Unifiable<?> out) {
		return goal.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	/**
	 * De Morgan over the sealed extension: one nogood per row at ONE —
	 * ground cells as equality literals, wide cells dropped, couplings as
	 * disequalities between the args. Conditional rows are translated by
	 * hand in their own receipt (the future negated() face's job).
	 */
	private static Goal negationOf(List<Tuple2<Reified<?>, Condition>> extension,
			Unifiable<?>... args) {
		return extension.stream()
				.map(answer -> rowNogood(answer, args))
				.reduce(Goal::and)
				.orElseGet(Goal::success);
	}

	@SuppressWarnings("unchecked")
	private static Goal rowNogood(Tuple2<Reified<?>, Condition> answer, Unifiable<?>[] args) {
		if (!Condition.ONE.equals(answer._2)) {
			throw new IllegalArgumentException("hand-translated in its own receipt: " + answer);
		}
		IndexedSeq<Term<Object>> cells = Answers.positions(answer._1);
		List<Posting> literals = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			Term<Object> cell = cells.get(i);
			if (cell.asVal().isDefined()) {
				literals.add(((Unifiable<Object>) args[i]).unifies(cell.get()));
				continue;
			}
			for (int j = 0; j < i; j++) {
				if (cells.get(j).equals(cell)) {
					literals.add(((Unifiable<Object>) args[i]).unifies((Unifiable<Object>) args[j]));
				}
			}
		}
		if (literals.isEmpty()) {
			// an all-wildcard row at ONE covers everything: the complement is empty
			return Goal.failure();
		}
		return exclude(literals.toArray(new Posting[0]));
	}

	@Test
	public void groundRowsNegateToForbiddenTuples() {
		TabledSource p = TabledSource.solving(args ->
				((Unifiable<Object>) args.get(0)).unifies(1)
						.and(((Unifiable<Object>) args.get(1)).unifies("a"))
						.or(((Unifiable<Object>) args.get(0)).unifies(2)
								.and(((Unifiable<Object>) args.get(1)).unifies("b"))));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Goal neg = negationOf(drain(p), x, y);
		assertThat(answers(neg.and(x.unifies(1)).and(y.unifies("a")), x)).isEmpty();
		assertThat(answers(neg.and(x.unifies(2)).and(y.unifies("b")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		assertThat(answers(negationOf(drain(p), x2, y2)
				.and(x2.unifies(2)).and(y2.unifies("c")), x2)).containsExactly("{2}");
	}

	@Test
	public void aWideRowNegatesToAStrongerExclusion() {
		// the positive row (1, Any) admits every tag, so the negation must
		// exclude item 1 outright — the Any DROPS from the nogood
		TabledSource p = TabledSource.solving(args ->
				((Unifiable<Object>) args.get(0)).unifies(1));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Goal neg = negationOf(drain(p), x, y);
		assertThat(answers(neg.and(x.unifies(1)).and(y.unifies("z")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		assertThat(answers(negationOf(drain(p), x2, lvar())
				.and(x2.unifies(2)), x2)).containsExactly("{2}");
	}

	@Test
	public void aCoupledRowNegatesToADisequality() {
		// the diagonal (Any₀, Any₀) negates to x ≠ y
		TabledSource p = TabledSource.solving(args ->
				((Unifiable<Object>) args.get(0)).unifies((Unifiable<Object>) args.get(1)));
		Unifiable<String> x = lvar();
		Unifiable<String> y = lvar();
		Goal neg = negationOf(drain(p), x, y);
		assertThat(answers(neg.and(x.unifies("v")).and(y.unifies("v")), x)).isEmpty();
		Unifiable<String> x2 = lvar();
		Unifiable<String> y2 = lvar();
		assertThat(answers(negationOf(drain(p), x2, y2)
				.and(x2.unifies("v")).and(y2.unifies("w")), x2)).containsExactly("{v}");
	}

	@Test
	public void aConditionalRowNegatesAsAFilter() {
		// the row (1, Any₁) under {Any₁ ≠ q} negates to the NESTED nogood
		// ¬(x≡1 ∧ ¬(y≡q)): the trial judges the inner literal three ways —
		// at y=q the inner exclusion is refuted, the conjunction cannot
		// hold, the nogood is crossed off and the branch PASSES. A filter
		// over the complement, never positive knowledge
		TabledSource p = TabledSource.solving(args ->
				((Unifiable<Object>) args.get(0)).unifies(1)
						.and(exclude(((Unifiable<Object>) args.get(1)).unifies("q"))));
		assertThat(drain(p).get(0)._2)
				.describedAs("the extension row is genuinely conditional")
				.isNotEqualTo(Condition.ONE);
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Goal neg = exclude(x.unifies(1), exclude(y.unifies("q")));
		assertThat(answers(neg.and(x.unifies(1)).and(y.unifies("z")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		assertThat(answers(exclude(x2.unifies(1), exclude(y2.unifies("q")))
				.and(x2.unifies(1)).and(y2.unifies("q")), x2)).containsExactly("{1}");
		Unifiable<Integer> x3 = lvar();
		Unifiable<String> y3 = lvar();
		assertThat(answers(exclude(x3.unifies(1), exclude(y3.unifies("q")))
				.and(x3.unifies(2)).and(y3.unifies("z")), x3)).containsExactly("{2}");
	}

	@Test
	public void negationIsConstructiveOverFreeVariables() {
		// no groundness demanded: with y bound and x free, the surviving
		// nogood reifies as the answer's residue — the complement described,
		// not enumerated
		TabledSource p = TabledSource.solving(args ->
				((Unifiable<Object>) args.get(0)).unifies(1)
						.and(((Unifiable<Object>) args.get(1)).unifies("a"))
						.or(((Unifiable<Object>) args.get(0)).unifies(2)
								.and(((Unifiable<Object>) args.get(1)).unifies("b"))));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(answers(negationOf(drain(p), x, y).and(y.unifies("a")), x))
				.containsExactly("_.0 : ¬(_.0 ≡ {1})");
	}
}
