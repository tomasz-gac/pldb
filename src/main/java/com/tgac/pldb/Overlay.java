package com.tgac.pldb;

// ABOUTME: A frozen base plus a private staged delta, read as one source — the
// ABOUTME: value semantics of an immutable store over a base that is merely shared.

import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.control.Try;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * An immutable store value over a shared read-only base: reads union the
 * base with this value's own staged facts, and every append mints a new
 * value — ancestors keep answering as before, siblings fork legally. The
 * staged delta (append order preserved) is what a commit face flushes;
 * until then the base never learns of it. Deletion is absent by design:
 * an event-sourced base has no deletes, so the refusal is the method's
 * absence, not a runtime check.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Overlay implements AnswerSource {

	private final AnswerSource base;
	private final Database delta;
	private final Array<Fact> staged;

	public static Overlay over(AnswerSource base) {
		return new Overlay(base, ImmutableDatabase.empty(), Array.empty());
	}

	public Try<Overlay> withFacts(List<Fact> facts) {
		return delta.withFacts(facts)
				.map(grown -> new Overlay(base, grown, staged.appendAll(facts)));
	}

	/** The facts this value's lineage appended, in append order. */
	public Array<Fact> staged() {
		return staged;
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		// TODO : There should be subsumption detection here if delta answers subsume base.
		// Equal rows (a staged duplicate of a committed fact) fold here; wide or
		// conditional delta rows shadowing base rows are the subsumption case above.
		Set<Tuple2<Reified<?>, Condition>> rows = new LinkedHashSet<>();
		for (Tuple2<Reified<?>, Condition> answer : base.answers(probe)) {
			rows.add(answer);
		}
		for (Tuple2<Reified<?>, Condition> answer : delta.answers(probe)) {
			rows.add(answer);
		}
		return rows;
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
