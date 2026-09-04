package com.tgac.pldb;

// ABOUTME: Constructive negation by composition: exclude(derived.posted(args)) — the
// ABOUTME: trial imposes the posted table on scratch and judges the complement.

import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Any goal compressed into a derived relation can be DENIED by plain
 * composition: {@code exclude(p.posted(x, y))} — the posted table is a
 * {@link com.tgac.logic.constraints.Posting}, so it is a nogood literal
 * like any other, and the trial imposes it on scratch and judges three
 * ways. The sealed extension is the completeness certificate (the closed
 * world is the relation's own definition), wide rows exclude harder, the
 * diagonal negates to a disequality, and a CONDITIONAL row filters: where
 * its guard refutes, the row cannot hold, the nogood crosses off and the
 * branch passes. A filter over the complement, never positive knowledge.
 */
public class DerivedNegationTest {

	private static final Property<Integer> item = Property.of("item");
	private static final Property<String> tag = Property.of("tag");

	private static final Relations._2<Integer, String> r =
			Relations.relation("r", item, tag);

	private static final Relations._1<Integer> s =
			Relations.relation("s", item);

	/** The exact answers for {@code out}, rendered and sorted. */
	private static List<String> answers(Goal goal, Unifiable<?> out) {
		return goal.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	public void groundRowsNegateToForbiddenTuples() {
		Relations._2<Integer, String>.Derived p = r.solving((i, t) ->
				i.unifies(1).and(t.unifies("a"))
						.or(i.unifies(2).and(t.unifies("b"))));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(answers(exclude(p.posted(x, y))
				.and(x.unifies(1)).and(y.unifies("a")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		assertThat(answers(exclude(p.posted(x2, y2))
				.and(x2.unifies(2)).and(y2.unifies("b")), x2)).isEmpty();
		Unifiable<Integer> x3 = lvar();
		Unifiable<String> y3 = lvar();
		assertThat(answers(exclude(p.posted(x3, y3))
				.and(x3.unifies(2)).and(y3.unifies("c")), x3)).containsExactly("{2}");
	}

	@Test
	public void aWideRowNegatesToAStrongerExclusion() {
		// the positive row (1, Any) admits every tag, so the negation
		// excludes item 1 outright
		Relations._2<Integer, String>.Derived p = r.solving((i, t) -> i.unifies(1));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(answers(exclude(p.posted(x, y))
				.and(x.unifies(1)).and(y.unifies("z")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		assertThat(answers(exclude(p.posted(x2, y2))
				.and(x2.unifies(2)).and(y2.unifies("z")), x2)).containsExactly("{2}");
	}

	@Test
	public void aCoupledRowNegatesToADisequality() {
		// the diagonal (Any₀, Any₀) negates to x ≠ y
		Relations._2<String, String> pair = Relations.relation("pair",
				Property.of("l"), Property.of("r"));
		Relations._2<String, String>.Derived p = pair.solving((l, rr) -> l.unifies(rr));
		Unifiable<String> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(answers(exclude(p.posted(x, y))
				.and(x.unifies("v")).and(y.unifies("v")), x)).isEmpty();
		Unifiable<String> x2 = lvar();
		Unifiable<String> y2 = lvar();
		assertThat(answers(exclude(p.posted(x2, y2))
				.and(x2.unifies("v")).and(y2.unifies("w")), x2)).containsExactly("{v}");
	}

	@Test
	public void aConditionalRowNegatesAsAFilter() {
		// row (1, Any₁) under {Any₁ ≠ q}: at (1,"q") the guard refutes, the
		// row cannot hold, the nogood crosses off and the branch PASSES —
		// the trial's three-way, never positive knowledge
		Relations._2<Integer, String>.Derived p = r.solving((i, t) ->
				i.unifies(1).and(exclude(t.unifies("q"))));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(answers(exclude(p.posted(x, y))
				.and(x.unifies(1)).and(y.unifies("z")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		assertThat(answers(exclude(p.posted(x2, y2))
				.and(x2.unifies(1)).and(y2.unifies("q")), x2)).containsExactly("{1}");
		Unifiable<Integer> x3 = lvar();
		Unifiable<String> y3 = lvar();
		assertThat(answers(exclude(p.posted(x3, y3))
				.and(x3.unifies(2)).and(y3.unifies("z")), x3)).containsExactly("{2}");
	}

	@Test
	public void negationIsConstructiveOverFreeVariables() {
		// no groundness demanded: with y bound and x free, the negation
		// survives as the answer's residue — the complement described,
		// not enumerated
		Relations._2<Integer, String>.Derived p = r.solving((i, t) ->
				i.unifies(1).and(t.unifies("a"))
						.or(i.unifies(2).and(t.unifies("b"))));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		List<String> free = answers(exclude(p.posted(x, y)).and(y.unifies("a")), x);
		assertThat(free).hasSize(1);
		assertThat(free.get(0)).startsWith("_.0 : ");
	}

	@Test
	public void aRecursiveDerivedRelationNegates() {
		// unreachability by composition: the closure seals (the fixpoint is
		// the inner tabling), and its complement filters — 1 reaches 3, so
		// (1,3) is excluded; 3 reaches nothing, so (3,1) passes
		Relations._2<Integer, Integer> reach = Relations.relation("reach",
				Property.of("src"), Property.of("dst"));
		Relations._2<Integer, Integer>.Derived p = reach.solvingRecursive(self -> (x, y) ->
				x.unifies(1).and(y.unifies(2))
						.or(x.unifies(2).and(y.unifies(3)))
						.or(defer(() -> {
							Unifiable<Integer> z = lvar();
							return self.apply(x, z)
									.and(z.unifies(2).and(y.unifies(3)));
						})));
		Unifiable<Integer> x = lvar();
		Unifiable<Integer> y = lvar();
		assertThat(answers(exclude(p.posted(x, y))
				.and(x.unifies(1)).and(y.unifies(3)), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<Integer> y2 = lvar();
		assertThat(answers(exclude(p.posted(x2, y2))
				.and(x2.unifies(3)).and(y2.unifies(1)), x2)).containsExactly("{3}");
	}

	@Test
	public void negatedAnyIsUnconditionalFailure() {
		Relations._1<Integer>.Derived p = s.solving((i) -> Goal.success());
		Unifiable<Integer> x = lvar();
		List<String> free = answers(exclude(p.posted(x)).and(x.unifies(2)), x);
		assertThat(free).hasSize(0);
	}
}
