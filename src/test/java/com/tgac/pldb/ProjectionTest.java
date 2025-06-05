package com.tgac.pldb;

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;

import com.tgac.logic.goals.Logic;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ProjectionTest {
	private static final Property<Integer> FIRST = Property.of("FIRST");
	private static final Property<Integer> SECOND = Property.of("SECOND");
	private static final Relations._2<Integer, Integer> PAIR =
			Relations.relation("pair",
					FIRST.indexed(),
					SECOND.indexed());

	Database DB = ImmutableDatabase.empty()
			.withFact(PAIR.fact(1, 6))
			.withFact(PAIR.fact(2, 5))
			.withFact(PAIR.fact(3, 4))
			.withFact(PAIR.fact(4, 3))
			.withFact(PAIR.fact(5, 2))
			.withFact(PAIR.fact(6, 1));

	@Test
	public void shouldGetAndProject(){
		Unifiable<Integer> first = lvar("first");
		Unifiable<Integer> second = lvar("second");
		Unifiable<Integer> x = lvar("x");

		Assertions.assertThat(PAIR.exists(DB, first, lval(6))
				.and(Logic.project(first, f -> x.unifies(f - 1)))
				.solve(x)
				.map(Unifiable::get)
				.collect(Collectors.toList()))
				.containsExactly(0);
	}

	@Test
	public void shouldProjectAndGet(){
		Unifiable<Integer> first = lvar("first");
		Unifiable<Integer> second = lvar("second");
		Unifiable<Integer> x = lvar("x");

		Assertions.assertThat(PAIR.exists(DB, first, lval(6))
				.and(Logic.project(first, f -> x.unifies(f - 1)))
				.solve(x)
				.map(Unifiable::get)
				.collect(Collectors.toList()))
				.containsExactly(0);
	}
}
