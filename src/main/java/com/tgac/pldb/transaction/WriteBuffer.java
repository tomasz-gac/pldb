package com.tgac.pldb.transaction;

// ABOUTME: A frozen base plus a private staged delta, read as one source — the
// ABOUTME: value semantics of an immutable store over a base that is merely shared.

import com.tgac.functional.Exceptions;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.JoinMap;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.AnswerStore;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import io.vavr.control.Try;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * An immutable store value over a shared read-only base: reads union the
 * base with this value's own staged facts, and every append mints a new
 * value — ancestors keep answering as before, siblings fork legally. The
 * staged delta (append order preserved) is what a commit face flushes;
 * until then the base never learns of it. Deletion is absent by design:
 * an event-sourced base has no deletes, so the refusal is the method's
 * absence, not a runtime check.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class WriteBuffer implements AnswerSource {
	AnswerSource base;
	AnswerStore delta;
	Array<Literal> staged;

	public static WriteBuffer over(AnswerSource base) {
		return new WriteBuffer(base, AnswerStore.empty(), Array.empty());
	}

	public Try<WriteBuffer> asserting(List<Literal> facts) {
		return delta.asserting(facts)
				.map(grown -> new WriteBuffer(base, grown, staged.appendAll(facts)));
	}

	/** The facts this value's lineage appended, in append order. */
	public Array<Literal> staged() {
		return staged;
	}

	@Override
	public Iterable<Answer> answers(Call<Relation> probe) {
		return overlay(probe, base.answers(probe));
	}

	/**
	 * The staged delta unioned over an already-fetched base — for callers
	 * that read the base themselves (a pinned read whose data must be the
	 * rows its pin certifies).
	 */
	public Iterable<Answer> overlay(Call<Relation> probe, Iterable<Answer> baseAnswers) {
		// a staged row that SUBSUMES a base row (image with consistent
		// bindings, condition absorbing) shadows it out of the delivery —
		// same-key duplicates still ⊕-fold in the cell below
		List<Answer> staged = StreamSupport.stream(delta.answers(probe).spliterator(), false)
				.collect(Collectors.toList());
		JoinMap<Reified<?>, Condition> folded = Stream.concat(
						StreamSupport.stream(baseAnswers.spliterator(), false)
								.filter(base -> staged.stream()
										.noneMatch(wide -> Answers.subsumes(wide, base))),
						staged.stream())
				.reduce(JoinMap.empty(Condition.RING),
						(map, answer) -> map.append(answer.getReified(), answer.getCondition()).getOrElse(map),
						Exceptions.throwingBiOp(UnsupportedOperationException::new));
		return IntStream.range(0, folded.size())
				.mapToObj(folded::get)
				.map(Answer::of)
				.collect(Collectors.toList());
	}

	@Override
	public long estimate(Call<Relation> probe) {
		return base.estimate(probe) + delta.estimate(probe);
	}

	/** The base names the world; the delta is this value's private view of it. */
	@Override
	public String id() {
		return base.id();
	}
}
