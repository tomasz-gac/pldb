package com.tgac.pldb.transaction;

// ABOUTME: The Transaction over the in-memory simulated serialization: the shared
// ABOUTME: cell is the one history, snapshots are values, the CAS is the commit lock.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.SharedDatabase;
import com.tgac.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class TransactionMemoryTest {

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation("person")
				.arg("id", id).indexed()
				.arg("name", name)
				.from(db);
	}

	private static Literal book(AnswerSource db, Unifiable<String> isbn, Unifiable<String> title) {
		return Literal.relation("book")
				.arg("isbn", isbn).indexed()
				.arg("title", title)
				.from(db);
	}

	private static List<String> names(AnswerSource db) {
		Unifiable<String> name = lvar();
		return person(db, lvar(), name).solve(name)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	private static List<String> titles(AnswerSource db) {
		Unifiable<String> title = lvar();
		return book(db, lvar(), title).solve(title)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	public void commitLandsStagedFactsForTheNextTransaction() throws Exception {
		SharedDatabase store = SharedDatabase.empty();
		Transaction writer = AbstractTransaction.over(store.open("w"))
				.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada")))).get();
		assertThat(names(writer)).containsExactly("{Ada}");
		assertThat(writer.commit().isSuccess()).isTrue();

		try (Transaction reader = AbstractTransaction.over(store.open("r"))) {
			assertThat(names(reader)).containsExactly("{Ada}");
		}
	}

	@Test
	public void writeSkewOnOneRelationMeetsTheConflict() throws Exception {
		SharedDatabase store = SharedDatabase.empty();
		Transaction first = AbstractTransaction.over(store.open("first"));
		Transaction second = AbstractTransaction.over(store.open("second"));
		assertThat(names(first)).isEmpty();
		assertThat(names(second)).isEmpty();

		first = first.withFacts(Collections.singletonList(
				person(null, lval(1), lval("Ada")))).get();
		second = second.withFacts(Collections.singletonList(
				person(null, lval(2), lval("Alan")))).get();

		assertThat(first.commit().isSuccess()).isTrue();
		Try<?> refused = second.commit();
		assertThat(refused.isFailure()).isTrue();
		assertThat(refused.getCause()).isInstanceOf(Transaction.Conflict.class);

		try (Transaction reader = AbstractTransaction.over(store.open("r"))) {
			assertThat(names(reader)).containsExactly("{Ada}");
		}
	}

	@Test
	public void disjointRelationsCommitWithoutConflict() throws Exception {
		SharedDatabase store = SharedDatabase.empty();
		try (
				Transaction seed = AbstractTransaction.over(store.open("seed"))
						.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada")))).get()
						.withFacts(Collections.singletonList(book(null, lval("978-0"), lval("SICP")))).get()
		) {
			assertThat(seed.commit().isSuccess()).isTrue();
		}

		Transaction bookWriter = AbstractTransaction.over(store.open("books"));
		Transaction personWriter = AbstractTransaction.over(store.open("people"));
		assertThat(titles(bookWriter)).containsExactly("{SICP}");
		assertThat(names(personWriter)).containsExactly("{Ada}");

		bookWriter = bookWriter.withFacts(Collections.singletonList(
				book(null, lval("978-1"), lval("TAPL")))).get();
		personWriter = personWriter.withFacts(Collections.singletonList(
				person(null, lval(2), lval("Alan")))).get();

		assertThat(bookWriter.commit().isSuccess()).isTrue();
		assertThat(personWriter.commit()
				.isSuccess())
				.describedAs("person marks never moved — the book commit must not bounce this one")
				.isTrue();
	}

	@Test
	public void readsAreStableAcrossAForeignCommit() throws Exception {
		// the snapshot property REST cannot promise and SQL buys with MVCC:
		// a commit landing between my open and my read must not appear
		SharedDatabase store = SharedDatabase.empty();
		Transaction reader = AbstractTransaction.over(store.open("reader"));

		try (
				Transaction writer = AbstractTransaction.over(store.open("writer"))
						.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada")))).get()
		) {
			assertThat(writer.commit().isSuccess()).isTrue();
		}

		assertThat(names(reader))
				.describedAs("the snapshot was taken at open, not at first read")
				.isEmpty();
		reader.close();
	}

	@Test
	public void anAbandonedTransactionLeavesNoTrace() throws Exception {
		SharedDatabase store = SharedDatabase.empty();
		try (
				Transaction abandoned = AbstractTransaction.over(store.open("a"))
						.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada")))).get()
		) {
			assertThat(names(abandoned)).containsExactly("{Ada}");
		}
		try (Transaction reader = AbstractTransaction.over(store.open("r"))) {
			assertThat(names(reader)).isEmpty();
		}
	}
}
