package com.tgac.pldb;

// ABOUTME: The sync kind lifted into the async seam: answers enumerated inline
// ABOUTME: inside the claimed workforce — the inline cost the sync kind always had.

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Emitter;
import com.tgac.functional.fibers.Fiber;
import com.tgac.logic.tabling.Call;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Relation;
import lombok.Value;

/**
 * A sync {@link AnswerSource} worn as an {@link AnswerProducer}: the
 * enumeration runs inline inside whoever drives the produce — blocking that
 * frame exactly as the sync consumption always did, emissions and
 * completion following in order. Consumers that can serve a sync face
 * cheaply (a sealed table read) may recognize the lift and keep the sync
 * lane synchronous end to end.
 */
@Value
class SyncLift implements AnswerProducer {

	AnswerSource source;

	@Override
	public Fiber<Nothing> produce(Call<Relation> probe, Emitter<Answer> emit) {
		Fiber<Nothing> emissions = Fiber.done(Nothing.nothing());
		for (Answer answer : source.answers(probe)) {
			emissions = emissions.flatMap(emitted -> emit.emit(answer));
		}
		return emissions;
	}

	@Override
	public long estimate(Call<Relation> probe) {
		return source.estimate(probe);
	}

	@Override
	public String id() {
		return source.id();
	}
}
