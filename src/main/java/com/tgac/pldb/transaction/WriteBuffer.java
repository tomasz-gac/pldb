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
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
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
	Database delta;
	Array<Literal> staged;

	public static WriteBuffer over(AnswerSource base) {
		return new WriteBuffer(base, ImmutableDatabase.empty(), Array.empty());
	}

	public Try<WriteBuffer> withFacts(List<Literal> facts) {
		return delta.withFacts(facts)
				.map(grown -> new WriteBuffer(base, grown, staged.appendAll(facts)));
	}

	/** The facts this value's lineage appended, in append order. */
	public Array<Literal> staged() {
		return staged;
	}

	@Override
	public Iterable<Answer> answers(Call<Relation> probe) {
		// TODO : There should be subsumption detection here if delta answers subsume base.
		// Same-key rows ⊕-fold in the cell (a staged duplicate is inert, conditions
		// join by absorption); a wide delta row shadowing a DIFFERENT base key is
		// the open subsumption case above.
		JoinMap<Reified<?>, Condition> folded = Stream.concat(
						StreamSupport.stream(base.answers(probe).spliterator(), false),
						StreamSupport.stream(delta.answers(probe).spliterator(), false))
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
