package com.tgac.pldb.relations;

// ABOUTME: Rule-backed literals: solving routes through the solve-scoped table,
// ABOUTME: value-equal mints share entries, method recursion seals, no self handle.

import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import io.vavr.Tuple;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.Test;

public class LiteralSolvingTest {

	private static Literal edge(AnswerSource db, Unifiable<Integer> src, Unifiable<Integer> dst) {
		return Literal.of("edge", db)
				.indexed("src", src)
				.indexed("dst", dst);
	}

	private static Database edges(int[][] pairs) {
		return ImmutableDatabase.empty()
				.withFacts(Arrays.stream(pairs)
						.map(p -> edge(null, lval(p[0]), lval(p[1])).fact())
						.collect(Collectors.toList()))
				.get();
	}

	/** The residence arc's target: recursion by calling the METHOD. */
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

	private static List<String> answers(Goal g, Unifiable<?> out) {
		return g.solve(out).map(Object::toString).sorted().collect(Collectors.toList());
	}

	@Test(timeout = 5000)
	public void aRuleLiteralEnumerates() {
		Database db = edges(new int[][]{{1, 2}, {1, 3}});
		Unifiable<Integer> x = lvar();
		Unifiable<Integer> y = lvar();
		Literal direct = Literal.solving("direct", edge(db, x, y))
				.arg("from", x)
				.arg("to", y);
		assertThat(answers(x.unifies(1).and(direct), y)).containsExactly("{2}", "{3}");
	}

	@Test(timeout = 5000)
	public void valueEqualMintsShareOneProduction() {
		Database db = edges(new int[][]{{1, 2}, {1, 3}});
		AtomicInteger productions = new AtomicInteger();
		Unifiable<Integer> one = lvar();
		Unifiable<Integer> a = lvar();
		Unifiable<Integer> b = lvar();
		// two mints of one definition: distinct Literal values, value-equal relations
		Goal first = counted(db, productions, one, a);
		Goal second = counted(db, productions, one, b);
		List<String> pairs = one.unifies(1).and(first).and(second)
				.solve(lval(Tuple.of(a, b)))
				.map(Term::get)
				.map(t -> t._1.get() + "," + t._2.get())
				.sorted()
				.collect(Collectors.toList());
		assertThat(pairs).containsExactly("2,2", "2,3", "3,2", "3,3");
		assertThat(productions.get()).isEqualTo(1);
	}

	private static Literal counted(AnswerSource db, AtomicInteger productions,
			Unifiable<Integer> x, Unifiable<Integer> y) {
		return Literal.solving("counted", defer(() -> {
					productions.incrementAndGet();
					return edge(db, x, y);
				}))
				.arg("from", x)
				.arg("to", y);
	}

	@Test(timeout = 5000)
	public void methodRecursionSealsAChain() {
		Database db = edges(new int[][]{{1, 2}, {2, 3}, {3, 4}});
		Unifiable<Integer> x = lvar();
		Unifiable<Integer> y = lvar();
		assertThat(answers(x.unifies(1).and(reach(db, x, y)), y))
				.containsExactly("{2}", "{3}", "{4}");
	}

	@Test(timeout = 5000)
	public void methodRecursionSealsACycle() {
		Database db = edges(new int[][]{{1, 2}, {2, 3}, {3, 1}});
		Unifiable<Integer> x = lvar();
		Unifiable<Integer> y = lvar();
		assertThat(answers(x.unifies(1).and(reach(db, x, y)), y))
				.containsExactly("{1}", "{2}", "{3}");
	}

	@Test(timeout = 5000)
	public void aDiamondFoldsDerivations() {
		Database db = edges(new int[][]{{1, 2}, {1, 3}, {2, 4}, {3, 4}});
		Unifiable<Integer> x = lvar();
		Unifiable<Integer> y = lvar();
		assertThat(answers(x.unifies(1).and(reach(db, x, y)), y))
				.containsExactly("{2}", "{3}", "{4}");
	}

	@Test(timeout = 5000)
	public void aHoistedLiteralServesTwoKeys() {
		Database db = edges(new int[][]{{1, 2}, {2, 3}});
		Unifiable<Integer> x = lvar();
		Unifiable<Integer> y = lvar();
		Literal hoisted = reach(db, x, y);
		// one mint, two branches, different key bindings per branch
		List<String> both = x.unifies(1).and(hoisted).and(y.unifies(2))
				.or(x.unifies(2).and(hoisted).and(y.unifies(3)))
				.solve(lval(Tuple.of(x, y)))
				.map(Term::get)
				.map(t -> t._1.get() + "," + t._2.get())
				.sorted().collect(Collectors.toList());
		assertThat(both).containsExactly("1,2", "2,3");
	}

	@Test(timeout = 5000)
	public void ruleAnswersAgreeWithDerivedSolving() {
		Database db = edges(new int[][]{{1, 2}, {1, 3}, {2, 4}});
		Relations._2<Integer, Integer> rel = Relations.relation("oracle",
				Property.<Integer>of("from"), Property.<Integer>of("to"));
		Relations._2<Integer, Integer>.Derived oracle = rel.solving((x, y) ->
				edge(db, x, y));
		Unifiable<Integer> a = lvar();
		Unifiable<Integer> b = lvar();
		Unifiable<Integer> x = lvar();
		Unifiable<Integer> y = lvar();
		Literal viaRule = Literal.solving("viaRule", edge(db, x, y))
				.arg("from", x).arg("to", y);
		assertThat(answers(x.unifies(1).and(viaRule), y))
				.containsExactly("{2}", "{3}")
				.isEqualTo(answers(a.unifies(1).and(oracle.exists(a, b)), b));
	}
}
