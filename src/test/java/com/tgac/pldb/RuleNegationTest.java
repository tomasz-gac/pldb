package com.tgac.pldb;

// ABOUTME: posted() over rule literals: the propagator reads the solve's shared
// ABOUTME: table — negation shapes, both dual-driver orders, recursive closure.

import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.relations.Literal;
import io.vavr.Tuple;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class RuleNegationTest {

	private static Literal p(Unifiable<Integer> i, Unifiable<String> t) {
		return Literal.solving("p",
						i.unifies(1).and(t.unifies("a"))
								.or(i.unifies(2).and(t.unifies("b"))))
				.arg("item", i)
				.arg("tag", t);
	}

	private static List<String> answers(Goal g, Unifiable<?> out) {
		return g.solve(out).map(Object::toString).sorted().collect(Collectors.toList());
	}

	@Test(timeout = 5000)
	public void groundRowsNegateToForbiddenTuples() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(answers(exclude(p(x, y)).and(x.unifies(1)).and(y.unifies("a")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		assertThat(answers(exclude(p(x2, y2)).and(x2.unifies(2)).and(y2.unifies("c")), x2))
				.containsExactly("{2}");
	}

	@Test(timeout = 5000)
	public void aWideRowNegatesToAStrongerExclusion() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Literal wide = Literal.solving("wide", x.unifies(1)).arg("item", x).arg("tag", y);
		assertThat(answers(exclude(wide).and(x.unifies(1)).and(y.unifies("z")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		Literal wide2 = Literal.solving("wide", x2.unifies(1)).arg("item", x2).arg("tag", y2);
		assertThat(answers(exclude(wide2).and(x2.unifies(2)).and(y2.unifies("z")), x2))
				.containsExactly("{2}");
	}

	@Test(timeout = 5000)
	public void aConditionalRowNegatesAsAFilter() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Literal guarded = Literal.solving("guarded",
						x.unifies(1).and(exclude(y.unifies("q"))))
				.arg("item", x).arg("tag", y);
		assertThat(answers(exclude(guarded).and(x.unifies(1)).and(y.unifies("z")), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		Literal guarded2 = Literal.solving("guarded",
						x2.unifies(1).and(exclude(y2.unifies("q"))))
				.arg("item", x2).arg("tag", y2);
		assertThat(answers(exclude(guarded2).and(x2.unifies(1)).and(y2.unifies("q")), x2))
				.containsExactly("{1}");
	}

	@Test(timeout = 5000)
	public void postedAgreesWithBareConsumption() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		List<String> viaPosted = answers(p(x, y).posted().and(x.unifies(1)), y);
		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		List<String> viaBare = answers(x2.unifies(1).and(p(x2, y2)), y2);
		assertThat(viaPosted).isEqualTo(viaBare).containsExactly("{a}");
	}

	@Test(timeout = 5000)
	public void postedThenConsumedAgree() {
		// the posting produces into its private table; the goal consumption
		// tables natively — two productions, one answer set
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Unifiable<Integer> a = lvar();
		Unifiable<String> b = lvar();
		List<String> both = p(x, y).posted().and(x.unifies(1))
				.and(p(a, b)).and(a.unifies(2))
				.solve(lval(Tuple.of(y, b)))
				.map(Term::get)
				.map(t -> t._1.get() + "," + t._2.get())
				.collect(Collectors.toList());
		assertThat(both).containsExactly("a,b");
	}

	@Test(timeout = 5000)
	public void consumedThenPostedAgree() {
		// order reversed: the two readings stay independent and agree
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Unifiable<Integer> a = lvar();
		Unifiable<String> b = lvar();
		List<String> both = x.unifies(1).and(p(x, y))
				.and(p(a, b).posted()).and(a.unifies(2))
				.solve(lval(Tuple.of(y, b)))
				.map(Term::get)
				.map(t -> t._1.get() + "," + t._2.get())
				.collect(Collectors.toList());
		assertThat(both).containsExactly("a,b");
	}

	@Test(timeout = 5000)
	public void aNegatedRecursiveClosureFilters() {
		Database db = ImmutableDatabase.empty()
				.withFacts(Arrays.asList(
						edge(null, lval(1), lval(2)).fact(),
						edge(null, lval(2), lval(3)).fact()))
				.get();
		// reachable: 1→2, 1→3, 2→3; NOT reachable: (3, anything), (2,1), ...
		Unifiable<Integer> x = lvar();
		Unifiable<Integer> y = lvar();
		assertThat(answers(exclude(reach(db, x, y))
				.and(x.unifies(1)).and(y.unifies(3)), x)).isEmpty();
		Unifiable<Integer> x2 = lvar();
		Unifiable<Integer> y2 = lvar();
		assertThat(answers(exclude(reach(db, x2, y2))
				.and(x2.unifies(2)).and(y2.unifies(1)), x2)).containsExactly("{2}");
	}

	private static Literal edge(AnswerSource db, Unifiable<Integer> src, Unifiable<Integer> dst) {
		return Literal.of("edge", db).indexed("src", src).indexed("dst", dst);
	}

	private static Literal reach(AnswerSource db, Unifiable<Integer> x, Unifiable<Integer> y) {
		return Literal.solving("reach",
						edge(db, x, y)
								.or(defer(() -> {
									Unifiable<Integer> z = lvar();
									return reach(db, x, z).and(edge(db, z, y));
								})))
				.arg("from", x)
				.arg("to", y);
	}
}
