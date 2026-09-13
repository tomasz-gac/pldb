package com.tgac.pldb.relations;

// ABOUTME: The projected() marker: inline ∃-projection — set semantics over the
// ABOUTME: kept columns, honest ¬∃ under exclude, a real join var inside bodies.

import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static com.tgac.pldb.relations.Projected.projected;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class ProjectedTest {

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation(ProjectedTest.class, "person")
				.arg("id", id).indexed()
				.arg("name", name)
				.from(db);
	}

	private static Literal edge(AnswerSource db, Unifiable<Integer> src, Unifiable<Integer> dst) {
		return Literal.relation(ProjectedTest.class, "edge")
				.arg("src", src).indexed()
				.arg("dst", dst)
				.from(db);
	}

	private static Database db() {
		return ImmutableDatabase.empty().withFacts(
				person(null, lval(1), lval("Ada")),
				person(null, lval(2), lval("Ada")),
				person(null, lval(3), lval("Kurt")),
				edge(null, lval(1), lval(2)),
				edge(null, lval(1), lval(3)),
				edge(null, lval(2), lval(3))).get();
	}

	@Test
	public void theProjectionDeliversSetSemantics() {
		// two ids share the name Ada: the projection onto name folds them —
		// bag semantics would deliver Ada twice
		Unifiable<String> name = lvar();
		List<String> names = person(db(), projected(), name).solve(name)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
		assertThat(names).containsExactly("{Ada}", "{Kurt}");
	}

	@Test
	public void negationOverTheProjectionIsHonestNotExists() {
		// the entry-1 semantics inline: ¬∃name.person(x, name) — a free lvar
		// here would hit the collapse pothole and the branch would survive
		Unifiable<Integer> x = lvar();
		assertThat(x.unifies(1)
				.and(exclude(person(db(), x, projected())))
				.solve(x)
				.collect(Collectors.toList())).isEmpty();

		Unifiable<Integer> y = lvar();
		assertThat(y.unifies(9)
				.and(exclude(person(db(), y, projected())))
				.solve(y)
				.map(Object::toString)
				.collect(Collectors.toList())).containsExactly("{9}");
	}

	@Test
	public void aProjectedLiteralJoinsLikeAnyOther() {
		// hasEdge(src) := edge(src, ∃dst) — two out-edges fold to one answer,
		// and the projection composes in a conjunction
		Unifiable<Integer> src = lvar();
		List<String> sources = edge(db(), src, projected())
				.and(person(db(), src, lvar()))
				.solve(src)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
		assertThat(sources).containsExactly("{1}", "{2}");
	}

	@Test
	public void negationOverAProjectedRuleIsHonestNotExists() {
		// the library's availableCopy shape: ¬∃via.linked(x, via) where
		// linked is itself a solving literal
		Unifiable<Integer> x = lvar();
		assertThat(x.unifies(1)
				.and(exclude(linked(db(), x, projected())))
				.solve(x)
				.collect(Collectors.toList())).isEmpty();

		Unifiable<Integer> y = lvar();
		assertThat(y.unifies(9)
				.and(exclude(linked(db(), y, projected())))
				.solve(y)
				.map(Object::toString)
				.collect(Collectors.toList())).containsExactly("{9}");
	}

	private static Literal linked(AnswerSource db, Unifiable<Integer> node, Unifiable<Integer> via) {
		return Literal.relation(ProjectedTest.class, "linked")
				.arg("node", node)
				.arg("via", via)
				.solving(edge(db, node, via));
	}

	/** The marker is a JOIN variable in the body: used twice, the projection
	 * must not sever the join — that is the leak that broke availableCopy. */
	private static Literal reciprocal(AnswerSource db, Unifiable<Integer> node, Unifiable<Integer> via) {
		return Literal.relation(ProjectedTest.class, "reciprocal")
				.arg("node", node)
				.arg("via", via)
				.solving(edge(db, node, via).and(edge(db, via, node)));
	}

	@Test
	public void aProjectedJoinVariableStaysOneVariable() {
		// no reciprocal pair exists, but node 2 has both an in-edge and an
		// out-edge — a severed join would deliver it.
		Unifiable<Integer> node = lvar();
		assertThat(reciprocal(db(), node, projected()).solve(node)
				.collect(Collectors.toList())).isEmpty();
	}

	@Test
	public void projectedOnAGroundColumnRefuses() {
		assertThatThrownBy(() -> Literal.relation(ProjectedTest.class, "person")
				.arg("id", projected()).ground()
				.arg("name", lvar())
				.from(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("id");
	}
}
