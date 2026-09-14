package com.tgac.pldb.transaction;

// ABOUTME: Answers-as-facts receipts: each answer grounds every template, constants
// ABOUTME: ride, clusters land whole, and a free cell refuses by relation and column.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Literal;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class UtilsTest {

	private static Literal loan(Unifiable<Integer> id, Unifiable<String> copy) {
		return Literal.relation(UtilsTest.class, "loan")
				.arg("loanId", id).indexed()
				.arg("copy", copy)
				.from(null);
	}

	private static Literal returned(Unifiable<Integer> id) {
		return Literal.relation(UtilsTest.class, "returned")
				.arg("loanId", id).indexed()
				.from(null);
	}

	private static String render(Fact fact) {
		return fact.getRelation().getName() + fact.getValues().toJavaList();
	}

	@Test
	public void oneAnswerGroundsTheWholeCluster() {
		Unifiable<Integer> id = lvar();
		Unifiable<String> copy = lvar();
		Goal question = id.unifies(1).and(copy.unifies("c1"));

		List<String> facts = Utils.persist(question, loan(id, copy), returned(id))
				.map(UtilsTest::render)
				.collect(Collectors.toList());

		assertThat(facts).containsExactly("loan[1, c1]", "returned[1]");
	}

	@Test
	public void everyAnswerContributesItsOwnCluster() {
		Unifiable<Integer> id = lvar();
		Unifiable<String> copy = lvar();
		Goal question = copy.unifies("c1")
				.and(id.unifies(1).or(id.unifies(2)));

		List<String> facts = Utils.persist(question, loan(id, copy), returned(id))
				.map(UtilsTest::render)
				.sorted()
				.collect(Collectors.toList());

		assertThat(facts).containsExactly(
				"loan[1, c1]", "loan[2, c1]", "returned[1]", "returned[2]");
	}

	@Test
	public void aConstantTemplateCellRidesIntoTheFact() {
		Unifiable<Integer> id = lvar();
		Goal question = id.unifies(7);

		List<String> facts = Utils.persist(question, loan(id, lval("archived")))
				.map(UtilsTest::render)
				.collect(Collectors.toList());

		assertThat(facts).containsExactly("loan[7, archived]");
	}

	@Test
	public void aFreeTemplateCellRefusesByRelationAndColumn() {
		Unifiable<Integer> id = lvar();
		Unifiable<String> copy = lvar();
		Goal question = id.unifies(1);

		assertThatThrownBy(() -> Utils.persist(question, loan(id, copy))
				.collect(Collectors.toList()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("loan")
				.hasMessageContaining("copy");
	}

	@Test
	public void zeroAnswersStreamNothing() {
		Unifiable<Integer> id = lvar();
		Goal question = id.unifies(1).and(id.unifies(2));

		assertThat(Utils.persist(question, loan(id, lval("c1")))
				.collect(Collectors.toList()))
				.isEmpty();
	}
}
