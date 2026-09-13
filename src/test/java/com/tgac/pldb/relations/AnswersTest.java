package com.tgac.pldb.relations;

// ABOUTME: The answer codec: a Fact encodes as (ground reified row, ONE); the image
// ABOUTME: decodes per position — cells in Term vocabulary, values for rows.

import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.tabling.Condition;
import io.vavr.collection.Array;
import org.junit.Test;

public class AnswersTest {

	private final Relation person = Literal.relation(AnswersTest.class, "person")
			.arg("id", lvar()).arg("name", lvar()).from(null).getRel();

	@Test
	public void aFactEncodesAsAGroundRowAtOne() {
		Fact fact = Fact.of(person, Array.of(1L, "Alan"));
		Answer answer = Answers.answer(fact);
		assertThat(answer.getCondition()).isEqualTo(Condition.ONE);
		assertThat(answer.getReified().isGround()).isTrue();
		assertThat(Answers.values(answer.getReified()).toJavaList()).containsExactly(1L, "Alan");
	}

	@Test
	public void valuesAreRawNotRendered() {
		// the braces gotcha: a reified term's toString renders decoration;
		// the codec hands back the VALUES, never their rendering
		Fact fact = Fact.of(person, Array.of(1L, "Alan"));
		Object first = Answers.values(Answers.answer(fact).getReified()).get(0);
		assertThat(first).isInstanceOf(Long.class).isEqualTo(1L);
	}

}
