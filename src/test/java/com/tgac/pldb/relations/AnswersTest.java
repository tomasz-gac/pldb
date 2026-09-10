package com.tgac.pldb.relations;

// ABOUTME: The answer codec: a Fact encodes as (ground reified row, ONE); the image
// ABOUTME: decodes per position — cells in Term vocabulary, values for rows.

import static com.tgac.logic.unification.LVar.lvar;

import static com.tgac.logic.unification.LVal.lval;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import java.util.Optional;
import org.junit.Test;

public class AnswersTest {

	private final Relation person = Literal.relation("person")
			.arg("id", lvar()).arg("name", lvar()).from(null).getRel();

	@Test
	public void aFactEncodesAsAGroundRowAtOne() {
		Fact fact = Fact.of(person, Array.of(1L, "Alan"));
		Tuple2<Reified<?>, Condition> answer = Answers.answer(fact);
		assertThat(answer._2).isEqualTo(Condition.ONE);
		assertThat(answer._1.isGround()).isTrue();
		assertThat(Answers.values(answer._1).toJavaList()).containsExactly(1L, "Alan");
	}

	@Test
	public void valuesAreRawNotRendered() {
		// the braces gotcha: a reified term's toString renders decoration;
		// the codec hands back the VALUES, never their rendering
		Fact fact = Fact.of(person, Array.of(1L, "Alan"));
		Object first = Answers.values(Answers.answer(fact)._1).get(0);
		assertThat(first).isInstanceOf(Long.class).isEqualTo(1L);
	}

}
