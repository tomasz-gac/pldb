package com.tgac.pldb.transaction;

// ABOUTME: The pin-before-read contract: a Simulated transaction pins a region at
// ABOUTME: its FIRST touch, before the read it certifies, and never twice.

import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.tabling.Call;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class PinAtFirstTouchTest {

	/** Records the order of its door calls; answers nothing. */
	private static final class Recording implements SimulatedSerialization {
		private final List<String> events = new ArrayList<>();

		@Override
		public Pin pin(Call<Relation> region) {
			events.add("pin:" + region.getRelation().getName());
			return new Pin() {
			};
		}

		@Override
		public boolean commit(Footprint read, List<Literal> flush) {
			events.add("commit:" + read.pins().size());
			return true;
		}

		@Override
		public Iterable<Answer> answers(Call<Relation> probe) {
			events.add("answers:" + probe.getRelation().getName());
			return Collections.emptyList();
		}

		@Override
		public long estimate(Call<Relation> probe) {
			return Long.MAX_VALUE;
		}

		@Override
		public String id() {
			return "recording";
		}

		@Override
		public void close() {
		}
	}

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation("person")
				.arg("id", id).indexed()
				.arg("name", name)
				.from(db);
	}

	@Test
	public void aRegionPinsAtFirstTouchBeforeItsReadAndOnlyOnce() throws Exception {
		Recording recording = new Recording();
		try (Transaction transaction = AbstractTransaction.over(recording)) {
			solve(transaction);
			solve(transaction);
			assertThat(transaction.commit().isSuccess()).isTrue();
		}
		assertThat(recording.events).containsExactly(
				"pin:person", "answers:person", "answers:person", "commit:1");
	}

	private static void solve(Transaction transaction) {
		Unifiable<String> name = lvar();
		assertThat(person(transaction, lvar(), name).solve(name)
				.collect(Collectors.toList())).isEmpty();
	}
}
