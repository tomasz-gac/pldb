package com.tgac.pldb;

// ABOUTME: The overlay value: a frozen base plus a private staged delta — reads
// ABOUTME: union both, appends mint new values, ancestors and siblings stay true.

import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Literal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class OverlayTest {

	private static Literal person(AnswerSource db, Unifiable<Long> id, Unifiable<String> name) {
		return Literal.relation("person").arg("id", id).indexed().arg("name", name).from(db);
	}

	private static Fact personFact(long id, String name) {
		return person(null, lval(id), lval(name)).fact();
	}

	private static AnswerSource base() {
		return ImmutableDatabase.empty().withFacts(Arrays.asList(
				personFact(1, "Ada"),
				personFact(2, "Alan"))).get();
	}

	private static List<String> ids(AnswerSource db) {
		Unifiable<Long> id = lvar();
		return person(db, id, lvar()).solve(id)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	public void readsUnionTheBaseAndTheStagedDelta() {
		Overlay lib = Overlay.over(base())
				.withFacts(Collections.singletonList(personFact(3, "Kurt"))).get();
		assertThat(ids(lib)).containsExactly("{1}", "{2}", "{3}");
	}

	@Test
	public void anAncestorIsUndisturbedByADescendantsAppend() {
		Overlay parent = Overlay.over(base());
		parent.withFacts(Collections.singletonList(personFact(3, "Kurt"))).get();
		assertThat(ids(parent)).containsExactly("{1}", "{2}");
	}

	@Test
	public void siblingsForkIndependently() {
		Overlay parent = Overlay.over(base());
		Overlay left = parent.withFacts(Collections.singletonList(personFact(3, "Kurt"))).get();
		Overlay right = parent.withFacts(Collections.singletonList(personFact(4, "Emmy"))).get();
		assertThat(ids(left)).containsExactly("{1}", "{2}", "{3}");
		assertThat(ids(right)).containsExactly("{1}", "{2}", "{4}");
	}

	@Test
	public void stagedFactsKeepAppendOrder() {
		Overlay lib = Overlay.over(base())
				.withFacts(Arrays.asList(personFact(3, "Kurt"), personFact(4, "Emmy"))).get()
				.withFacts(Collections.singletonList(personFact(5, "Noether"))).get();
		assertThat(lib.staged()).containsExactly(
				personFact(3, "Kurt"), personFact(4, "Emmy"), personFact(5, "Noether"));
	}

	@Test
	public void aFreshOverlayStagesNothing() {
		assertThat(Overlay.over(base()).staged()).isEmpty();
	}

	@Test
	public void negationSeesTheStagedDelta() {
		// the constraint tier reads through the same union: a staged row
		// entails a ground exclusion the base alone would have refuted
		Overlay lib = Overlay.over(base())
				.withFacts(Collections.singletonList(personFact(3, "Kurt"))).get();
		Unifiable<Long> free = lvar();
		assertThat(free.unifies(3L)
				.and(exclude(person(lib, free, lval("Kurt"))))
				.solve(free)
				.collect(Collectors.toList())).isEmpty();
		assertThat(free.unifies(3L)
				.and(exclude(person(Overlay.over(base()), free, lval("Kurt"))))
				.solve(free)
				.map(Object::toString)
				.collect(Collectors.toList())).containsExactly("{3}");
	}
}
