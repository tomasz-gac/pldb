package com.tgac.pldb.transaction;

// ABOUTME: The premise: a client's earlier pinned reads carried into a later commit —
// ABOUTME: certified beside the transaction's own ledger, refusing if that world moved.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.SharedDatabase;
import com.tgac.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.Test;

public class PremiseTest {

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation(PremiseTest.class, "person")
				.arg("id", id).indexed()
				.arg("name", name)
				.from(db);
	}

	private static Literal book(AnswerSource db, Unifiable<String> isbn, Unifiable<String> title) {
		return Literal.relation(PremiseTest.class, "book")
				.arg("isbn", isbn).indexed()
				.arg("title", title)
				.from(db);
	}

	private static void solveNames(AnswerSource db) {
		Unifiable<String> name = lvar();
		person(db, lvar(), name).solve(name).collect(Collectors.toList());
	}

	private static void solveTitles(AnswerSource db) {
		Unifiable<String> title = lvar();
		book(db, lvar(), title).solve(title).collect(Collectors.toList());
	}

	@Test
	public void theFootprintExposesEveryReadIncludingTheEmptyOne() throws Exception {
		SharedDatabase store = SharedDatabase.empty();
		assertThat(AbstractTransaction.over(store.open("seed"))
				.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada"))))
				.get().commit().isSuccess()).isTrue();

		try (Simulated get = AbstractTransaction.over(store.open("get"))) {
			solveNames(get);
			solveTitles(get);
			assertThat(get.footprint().pins().keySet().stream()
					.map(region -> region.getRelation().getName())
					.distinct())
					.describedAs("the populated read AND the empty one both pin their regions")
					.containsExactlyInAnyOrder("person", "book");
		}
	}

	@Test
	public void aFreshPremiseLandsThroughACommitThatNeverReadIt() throws Exception {
		SharedDatabase store = SharedDatabase.empty();
		Footprint premise;
		try (Simulated get = AbstractTransaction.over(store.open("get"))) {
			solveNames(get);
			premise = get.footprint();
		}

		// the pure-premise posture: the committing transaction reads NOTHING —
		// person is certified only because the client's premise carried it
		Try<?> landed = AbstractTransaction.over(store.open("post"))
				.requiring(premise)
				.withFacts(Collections.singletonList(book(null, lval("i1"), lval("Tar Pit"))))
				.get().commit();
		assertThat(landed.isSuccess()).isTrue();
	}

	@Test
	public void aStalePremiseRefusesTheCommitEvenThoughItsOwnReadsHold() throws Exception {
		SharedDatabase store = SharedDatabase.empty();
		Footprint premise;
		try (Simulated get = AbstractTransaction.over(store.open("get"))) {
			solveNames(get);
			premise = get.footprint();
		}

		assertThat(AbstractTransaction.over(store.open("mover"))
				.withFacts(Collections.singletonList(person(null, lval(2), lval("Alan"))))
				.get().commit().isSuccess()).isTrue();

		Try<?> refused = AbstractTransaction.over(store.open("post"))
				.requiring(premise)
				.withFacts(Collections.singletonList(book(null, lval("i1"), lval("Tar Pit"))))
				.get().commit();
		assertThat(refused.isFailure()).isTrue();
		assertThat(refused.getCause())
				.describedAs("the world the client decided against has moved — re-read, re-solve")
				.isInstanceOf(Transaction.Conflict.class);
	}
}
