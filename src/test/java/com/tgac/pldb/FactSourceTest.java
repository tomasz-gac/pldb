package com.tgac.pldb;

// ABOUTME: Pins the FactSource seam: lookups and posted constraints constructed
// ABOUTME: against the read face answer identically to the Database-typed path.

import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Test;

public class FactSourceTest {

	private static final Property<Integer> id = Property.of("id");
	private static final Property<String> name = Property.of("name");

	private static final Relations._2<Integer, String> person =
			Relations.relation("person", id.indexed(), name);

	private static final Database db = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					person.fact(1, "Ada"),
					person.fact(2, "Alan"),
					person.fact(3, "Kurt")))
			.get();

	@Test
	public void aLookupThroughTheSeamAnswersLikeTheDatabase() {
		FactSource source = db;

		Unifiable<String> viaSource = lvar();
		Unifiable<String> viaDb = lvar();
		assertThat(person.exists(source, lvar(), viaSource)
				.solve(viaSource)
				.map(Object::toString)
				.collect(Collectors.toList()))
				.containsExactlyElementsOf(person.exists(db, lvar(), viaDb)
						.solve(viaDb)
						.map(Object::toString)
						.collect(Collectors.toList()));
	}

	@Test
	public void aPostedConstraintThroughTheSeamAnswersLikeTheDatabase() {
		FactSource source = db;

		Unifiable<Integer> keyViaSource = lvar();
		Unifiable<Integer> keyViaDb = lvar();
		assertThat(person.posted(source, keyViaSource, lvar())
				.solve(keyViaSource)
				.map(Object::toString)
				.collect(Collectors.toList()))
				.containsExactlyElementsOf(person.posted(db, keyViaDb, lvar())
						.solve(keyViaDb)
						.map(Object::toString)
						.collect(Collectors.toList()));
	}
}
