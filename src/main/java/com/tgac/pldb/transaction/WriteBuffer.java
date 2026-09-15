package com.tgac.pldb.transaction;

// ABOUTME: A frozen base plus a private SIGNED delta — staged assertions and staged
// ABOUTME: retractions — read as one source with the value semantics of a store.

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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * An immutable store value over a shared read-only base: reads union the
 * base with this value's staged assertions MINUS its staged retractions,
 * and every write mints a new value — ancestors keep answering as
 * before, siblings fork legally. The two staged lists (order preserved)
 * are what a commit face lands; until then the base never learns of
 * either. A fact staged both ways refuses as {@link Transaction.Conflict}:
 * the transaction has not decided what it believes. The DOMAIN's
 * no-delete doctrine lives above this type, at the facades that choose
 * not to expose the retracting door.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class WriteBuffer implements AnswerSource {
	AnswerSource base;
	AnswerStore delta;
	AnswerStore removals;
	Array<Literal> stagedAssertions;
	Array<Literal> stagedRetractions;

	public static WriteBuffer over(AnswerSource base) {
		return new WriteBuffer(base, AnswerStore.empty(), AnswerStore.empty(),
				Array.empty(), Array.empty());
	}

	public Try<WriteBuffer> asserting(List<Literal> facts) {
		return refuseCollision(removals, facts)
				.flatMap(clear -> delta.asserting(facts))
				.map(grown -> new WriteBuffer(base, grown, removals,
						stagedAssertions.appendAll(facts), stagedRetractions));
	}

	public Try<WriteBuffer> retracting(Collection<Literal> facts) {
		return refuseCollision(delta, facts)
				.flatMap(clear -> removals.asserting(facts))
				.map(marked -> new WriteBuffer(base, delta, marked,
						stagedAssertions, stagedRetractions.appendAll(facts)));
	}

	/** A fact staged with the opposite polarity refuses the write whole. */
	private static Try<AnswerStore> refuseCollision(AnswerStore opposite, Collection<Literal> facts) {
		return Try.of(() -> {
			for (Literal fact : facts) {
				Reified<?> image = Answers.answer(fact.fact()).getReified();
				if (opposite.answers(Call.of(fact.getRel(), image)).iterator().hasNext()) {
					throw new Transaction.Conflict("the fact " + fact.getRel().getName()
							+ image + " is staged with the opposite polarity —"
							+ " this transaction has not decided what it believes");
				}
			}
			return opposite;
		});
	}

	/** The facts this value's lineage staged to land, in staging order. */
	public Array<Literal> stagedAssertions() {
		return stagedAssertions;
	}

	/** The facts this value's lineage staged to remove, in staging order. */
	public Array<Literal> stagedRetractions() {
		return stagedRetractions;
	}

	@Override
	public Iterable<Answer> answers(Call<Relation> probe) {
		return overlay(probe, base.answers(probe));
	}

	/**
	 * The signed delta over an already-fetched base — for callers that
	 * read the base themselves (a pinned read whose data must be the rows
	 * its pin certifies). A staged retraction hides its base row by image;
	 * a staged row that SUBSUMES a base row shadows it out of the
	 * delivery; same-key duplicates still ⊕-fold in the cell below.
	 */
	public Iterable<Answer> overlay(Call<Relation> probe, Iterable<Answer> baseAnswers) {
		List<Answer> staged = StreamSupport.stream(delta.answers(probe).spliterator(), false)
				.collect(Collectors.toList());
		Set<Reified<?>> removed = StreamSupport.stream(removals.answers(probe).spliterator(), false)
				.map(Answer::getReified)
				.collect(Collectors.toCollection(HashSet::new));
		JoinMap<Reified<?>, Condition> folded = Stream.concat(
						StreamSupport.stream(baseAnswers.spliterator(), false)
								.filter(base -> !removed.contains(base.getReified()))
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

	/** An upper bound: retractions never lower it — over-estimation is sound. */
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
