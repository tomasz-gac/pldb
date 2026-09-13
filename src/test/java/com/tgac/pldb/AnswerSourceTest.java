package com.tgac.pldb;

// ABOUTME: Pins the AnswerSource seam: lookups and posted constraints constructed
// ABOUTME: against the read face answer identically to the AnswerStore-typed path.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.inmemory.AnswerStore;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Test;

public class AnswerSourceTest {

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation(AnswerSourceTest.class, "person")
				.arg("id", id).indexed()
				.arg("name", name)
				.from(db);
	}

	private static Relation personRel() {
		return person(null, lvar(), lvar()).getRel();
	}

	private static final Property<Integer> id = Property.of("id");
	private static final Property<String> name = Property.of("name");

	private static final AnswerStore db = AnswerStore.empty()
			.withFacts(Arrays.asList(
					person(null, lval(1), lval("Ada")),
					person(null, lval(2), lval("Alan")),
					person(null, lval(3), lval("Kurt"))))
			.get();

	@Test
	public void aLookupThroughTheSeamAnswersLikeTheDatabase() {
		AnswerSource source = db;

		Unifiable<String> viaSource = lvar();
		Unifiable<String> viaDb = lvar();
		assertThat(person(source, lvar(), viaSource)
				.solve(viaSource)
				.map(Object::toString)
				.collect(Collectors.toList()))
				.containsExactlyElementsOf(person(db, lvar(), viaDb)
						.solve(viaDb)
						.map(Object::toString)
						.collect(Collectors.toList()));
	}

	@Test
	public void aPostedConstraintThroughTheSeamAnswersLikeTheDatabase() {
		AnswerSource source = db;

		Unifiable<Integer> keyViaSource = lvar();
		Unifiable<Integer> keyViaDb = lvar();
		assertThat(person(source, keyViaSource, lvar()).posted()
				.solve(keyViaSource)
				.map(Object::toString)
				.collect(Collectors.toList()))
				.containsExactlyElementsOf(person(db, keyViaDb, lvar()).posted()
						.solve(keyViaDb)
						.map(Object::toString)
						.collect(Collectors.toList()));
	}
}
