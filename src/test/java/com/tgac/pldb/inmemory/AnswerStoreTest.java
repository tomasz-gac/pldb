package com.tgac.pldb.inmemory;

// ABOUTME: The Call-native store's receipts: buckets serve ground probes, null and
// ABOUTME: wide rows key honestly, duplicate images ⊕-fold, values fork persistent.

import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.fibers.schedulers.BreadthFirstScheduler;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.Table;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.GoalProducer;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

	private static List<String> images(Iterable<Answer> answers) {
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

	/** A REAL condition, minted by a guarded produce: ¬(member ≡ forbidden). */
	private static Condition forbidding(String forbidden) {
		Unifiable<Object> member = lvar();
		Unifiable<Object> copy = lvar();
		GoalProducer guarded = GoalProducer.of(LOAN,
				exclude(member.unifies(forbidden)), Array.of(member, copy), Table.empty());
		List<Answer> delivered = new ArrayList<>();
		new BreadthFirstScheduler<>(guarded.produce(probe(Any.of(0), Any.of(1)), answer -> {
			delivered.add(answer);
			return Fiber.done(Nothing.nothing());
		})).get();
		return delivered.get(0).getCondition();
	}

	@Test
	public void aConditionalAnswerLandsAndKeepsItsCondition() {
		// the ground-pool refusal dies here: the store speaks the seam's
		// whole entry shape, conditions included
		Condition guarded = forbidding("m9");
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, Answer.of(row("m1", "c1").getReified(), guarded));

		assertThat(store.answers(probe(lval("m1"), Any.of(1))).iterator().next().getCondition())
				.isEqualTo(guarded);
	}

	@Test
	public void oneImageUnderTwoGuardsFoldsToTheirDisjunction() {
		// the c-table union law with REAL conditions: membership holds if
		// EITHER derivation's guard does — the folded condition is A ⊕ B,
		// not A, not B, not ONE
		Condition notM8 = forbidding("m8");
		Condition notM9 = forbidding("m9");
		assertThat(notM8).isNotEqualTo(notM9);

		Reified<?> image = row("m1", "c1").getReified();
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, Answer.of(image, notM8))
				.with(LOAN, Answer.of(image, notM9));

		Iterable<Answer> answers = store.answers(probe(lval("m1"), Any.of(1)));
		assertThat(images(answers)).hasSize(1);
		Condition folded = answers.iterator().next().getCondition();
		assertThat(folded).isEqualTo(Condition.RING.plus(notM8, notM9));
		assertThat(folded).isNotEqualTo(notM8);
		assertThat(folded).isNotEqualTo(notM9);
		assertThat(folded).isNotEqualTo(Condition.ONE);
	}

	@Test
	public void aNarrowInsertServesTheWideRead() {
		// membership is direction-free: rows fetched under narrow probes are
		// rows of the RELATION — the wide probe sees them all; only coverage
		// (beside the store) may claim it saw everything
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, row("m1", "c1"))
				.with(LOAN, row("m2", "c2"));

		assertThat(images(store.answers(probe(Any.of(0), Any.of(1)))))
				.containsExactly("{Array({m1}, {c1})}", "{Array({m2}, {c2})}");
	}

	@Test
	public void overlappingWideImagesKeepTheirOwnGuards() {
		// loan(m1,_) under A and loan(_,c3) under B: their common instance
		// loan(m1,c3) is claimed by BOTH — the store delivers both rows with
		// their OWN conditions, and the instance's A ⊕ B emerges at delivery
		// (either branch restates); no cross-image fold, ever — images are
		// distinct claims, not duplicate derivations
		Condition a = forbidding("m8");
		Condition b = forbidding("m9");
		Reified<?> memberWide = (Reified<?>) lval(Array.of(
				(Term<Object>) (Term<?>) lval("m1"), Any.of(0)));
		Reified<?> copyWide = (Reified<?>) lval(Array.of(
				Any.of(0), (Term<Object>) (Term<?>) lval("c3")));
		AnswerStore store = AnswerStore.empty()
				.with(LOAN, Answer.of(memberWide, a))
				.with(LOAN, Answer.of(copyWide, b));

		Iterable<Answer> overlap = store.answers(probe(lval("m1"), lval("c3")));
		assertThat(images(overlap)).containsExactly("{Array({m1}, _.0)}", "{Array(_.0, {c3})}");
		Iterator<Answer> both = overlap.iterator();
		assertThat(both.next().getCondition()).isEqualTo(a);
		assertThat(both.next().getCondition()).isEqualTo(b);

		assertThat(images(store.answers(probe(lval("m9"), lval("c3")))))
				.describedAs("outside the member-wide claim, only the copy-wide row answers")
				.containsExactly("{Array(_.0, {c3})}");
	}

	@Test
	public void insertsForkTheValueAndAncestorsKeepAnswering() {
		AnswerStore before = AnswerStore.empty().with(LOAN, row("m1", "c1"));
		AnswerStore after = before.with(LOAN, row("m1", "c2"));

		assertThat(images(before.answers(probe(lval("m1"), Any.of(1))))).hasSize(1);
		assertThat(images(after.answers(probe(lval("m1"), Any.of(1))))).hasSize(2);
	}
}
