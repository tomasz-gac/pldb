package com.tgac.pldb;

// ABOUTME: The overlay value: a frozen base plus a private staged delta — reads
// ABOUTME: union both, appends mint new values, ancestors and siblings stay true.

import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.transaction.WriteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class WriteBufferTest {

	private static Literal person(AnswerSource db, Unifiable<Long> id, Unifiable<String> name) {
		return Literal.relation("person").arg("id", id).indexed().arg("name", name).from(db);
	}

	private static AnswerSource base() {
		return ImmutableDatabase.empty().withFacts(Arrays.asList(
				person(null, lval(1L), lval("Ada")),
				person(null, lval(2L), lval("Alan")))).get();
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
		WriteBuffer lib = WriteBuffer.over(base())
				.withFacts(Collections.singletonList(person(null, lval(3L), lval("Kurt")))).get();
		assertThat(ids(lib)).containsExactly("{1}", "{2}", "{3}");
	}

	@Test
	public void anAncestorIsUndisturbedByADescendantsAppend() {
		WriteBuffer parent = WriteBuffer.over(base());
		parent.withFacts(Collections.singletonList(person(null, lval(3L), lval("Kurt")))).get();
		assertThat(ids(parent)).containsExactly("{1}", "{2}");
	}

	@Test
	public void siblingsForkIndependently() {
		WriteBuffer parent = WriteBuffer.over(base());
		WriteBuffer left = parent.withFacts(Collections.singletonList(person(null, lval(3L), lval("Kurt")))).get();
		WriteBuffer right = parent.withFacts(Collections.singletonList(person(null, lval(4L), lval("Emmy")))).get();
		assertThat(ids(left)).containsExactly("{1}", "{2}", "{3}");
		assertThat(ids(right)).containsExactly("{1}", "{2}", "{4}");
	}

	@Test
	public void stagedFactsKeepAppendOrder() {
		WriteBuffer lib = WriteBuffer.over(base())
				.withFacts(Arrays.asList(person(null, lval(3L), lval("Kurt")), person(null, lval(4L), lval("Emmy")))).get()
				.withFacts(Collections.singletonList(person(null, lval(5L), lval("Noether")))).get();
		assertThat(lib.staged().map(com.tgac.pldb.relations.Literal::fact)).containsExactly(
				person(null, lval(3L), lval("Kurt")).fact(),
				person(null, lval(4L), lval("Emmy")).fact(),
				person(null, lval(5L), lval("Noether")).fact());
	}

	@Test
	public void aStagedDuplicateOfACommittedFactDeliversOnce() {
		WriteBuffer lib = WriteBuffer.over(base())
				.withFacts(Collections.singletonList(person(null, lval(1L), lval("Ada")))).get();
		assertThat(ids(lib)).containsExactly("{1}", "{2}");
	}

	@Test
	public void aFreshOverlayStagesNothing() {
		assertThat(WriteBuffer.over(base()).staged()).isEmpty();
	}

	@Test
	public void negationSeesTheStagedDelta() {
		// the constraint tier reads through the same union: a staged row
		// entails a ground exclusion the base alone would have refuted
		WriteBuffer lib = WriteBuffer.over(base())
				.withFacts(Collections.singletonList(person(null, lval(3L), lval("Kurt")))).get();
		Unifiable<Long> free = lvar();
		assertThat(free.unifies(3L)
				.and(exclude(person(lib, free, lval("Kurt"))))
				.solve(free)
				.collect(Collectors.toList())).isEmpty();
		assertThat(free.unifies(3L)
				.and(exclude(person(WriteBuffer.over(base()), free, lval("Kurt"))))
				.solve(free)
				.map(Object::toString)
				.collect(Collectors.toList())).containsExactly("{3}");
	}
}
