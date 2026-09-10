package com.tgac.pldb.inmemory;

// ABOUTME: In-memory null cells through the answers(Call) face: free probes
// ABOUTME: deliver {null}, bound-null probes select, indexed null cells key.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class NullCellTest {

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation("person")
				.arg("id", id).indexed()
				.arg("name", name).nullable()
				.from(db);
	}

	/** name INDEXED and nullable: a null cell must key its bucket. */
	private static Literal tagged(AnswerSource db, Unifiable<Integer> id, Unifiable<String> tag) {
		return Literal.relation("tagged")
				.arg("id", id).indexed()
				.arg("tag", tag).indexed().nullable()
				.from(db);
	}

	private static Database db() {
		return ImmutableDatabase.empty().withFacts(Arrays.asList(
				person(null, lval(1), lval("Ada")),
				person(null, lval(2), lval((String) null)),
				tagged(null, lval(1), lval("core")),
				tagged(null, lval(2), lval((String) null)))).get();
	}

	@Test
	public void aFreeProbeDeliversTheNullCell() {
		Unifiable<String> name = lvar();
		List<String> names = person(db(), lvar(), name).solve(name)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
		assertThat(names).containsExactly("{Ada}", "{null}");
	}

	@Test
	public void aBoundNullProbeSelectsOnlyNullRows() {
		Unifiable<Integer> id = lvar();
		assertThat(person(db(), id, lval((String) null)).solve(id)
				.map(Object::toString)
				.collect(Collectors.toList())).containsExactly("{2}");
	}

	@Test
	public void aBoundNullProbeOnAnIndexedColumnSelects() {
		Unifiable<Integer> id = lvar();
		assertThat(tagged(db(), id, lval((String) null)).solve(id)
				.map(Object::toString)
				.collect(Collectors.toList())).containsExactly("{2}");
	}
}
