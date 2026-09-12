package com.tgac.pldb.inmemory;

// ABOUTME: The Call-native store's receipts: buckets serve ground probes, null and
// ABOUTME: wide rows key honestly, duplicate images ⊕-fold, values fork persistent.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Test;

public class AnswerStoreTest {

	private static final Relation LOAN = Literal.relation("loan")
			.arg("member", lvar()).indexed()
			.arg("copy", lvar())
			.from(null)
			.getRel();

	private static Answer row(Object member, Object copy) {
		return Answers.answer(Fact.of(LOAN, Array.of(member, copy)));
	}

	private static Call<Relation> probe(Term<?> member, Term<?> copy) {
		return Call.of(LOAN, (Reified<?>) lval(Array.of(member, copy)));
	}

	private static java.util.List<String> images(Iterable<Answer> answers) {
		return StreamSupport.stream(answers.spliterator(), false)
				.map(answer -> answer.getReified().toString())
				.collect(Collectors.toList());
	}

	@Test
	public void aGroundIndexedProbeServesItsBucket() {
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, row("m1", "c1"))
				.with(LOAN, row("m1", "c2"))
				.with(LOAN, row("m2", "c3"));

		assertThat(images(store.answers(probe(lval("m1"), Any.of(1)))))
				.containsExactly("{Array({m1}, {c1})}", "{Array({m1}, {c2})}");
		assertThat(store.estimate(probe(lval("m1"), Any.of(1)))).isEqualTo(2);
		assertThat(store.estimate(probe(Any.of(0), Any.of(1)))).isEqualTo(3);
	}

	@Test
	public void aNonIndexedBoundPositionFilters() {
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, row("m1", "c1"))
				.with(LOAN, row("m1", "c2"));

		assertThat(images(store.answers(probe(lval("m1"), lval("c2")))))
				.containsExactly("{Array({m1}, {c2})}");
	}

	@Test
	public void nullIsAnHonestBucketKey() {
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, row(null, "c1"))
				.with(LOAN, row("m1", "c2"));

		assertThat(images(store.answers(probe(lval(null), Any.of(1)))))
				.containsExactly("{Array({null}, {c1})}");
	}

	@Test
	public void aRowFreeAtAnIndexedColumnMatchesEveryProbe() {
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, row("m1", "c1"))
				.with(LOAN, Answer.of(
						(Reified<?>) lval(Array.of(Any.of(0), (Term<Object>) (Term<?>) lval("c9"))),
						Condition.ONE));

		assertThat(images(store.answers(probe(lval("m2"), Any.of(1)))))
				.describedAs("the wide row lives in the wildcard set — every member probe sees it")
				.containsExactly("{Array(_.0, {c9})}");
	}

	@Test
	public void aDuplicateImageFoldsItsConditionInsteadOfDuplicating() {
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, row("m1", "c1"))
				.with(LOAN, row("m1", "c1"));

		Iterable<Answer> answers = store.answers(probe(lval("m1"), Any.of(1)));
		assertThat(images(answers)).hasSize(1);
		assertThat(answers.iterator().next().getCondition()).isEqualTo(Condition.ONE);
	}

	@Test
	public void aConditionalAnswerLandsAndKeepsItsCondition() {
		// the ground-pool refusal dies here: the store speaks the seam's
		// whole entry shape, conditions included
		Condition guarded = Condition.of(com.tgac.logic.tabling.Residues.TRUE);
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, Answer.of(row("m1", "c1").getReified(), guarded));

		assertThat(store.answers(probe(lval("m1"), Any.of(1))).iterator().next().getCondition())
				.isEqualTo(guarded);
	}

	@Test
	public void insertsForkTheValueAndAncestorsKeepAnswering() {
		AnswerStore before = AnswerStore.empty().with(LOAN, row("m1", "c1"));
		AnswerStore after = before.with(LOAN, row("m1", "c2"));

		assertThat(images(before.answers(probe(lval("m1"), Any.of(1))))).hasSize(1);
		assertThat(images(after.answers(probe(lval("m1"), Any.of(1))))).hasSize(2);
	}
}
