package org.clauseway.pldb.transaction;

// ABOUTME: The Transaction over the in-memory simulated serialization: the shared
// ABOUTME: cell is the one history, snapshots are values, the CAS is the commit lock.

import static org.clauseway.logic.unification.LVal.lval;
import static org.clauseway.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import org.clauseway.logic.unification.Unifiable;
import org.clauseway.pldb.AnswerSource;
import org.clauseway.pldb.inmemory.SharedDatabase;
import org.clauseway.functional.fibers.schedulers.BreadthFirstScheduler;
import org.clauseway.pldb.relations.Answer;
import org.clauseway.pldb.relations.Question;
import org.clauseway.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class TransactionMemoryTest {

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation(TransactionMemoryTest.class, "person")
				.arg("id", id).indexed()
				.arg("name", name)
				.from(db);
	}

	private static Literal book(AnswerSource db, Unifiable<String> isbn, Unifiable<String> title) {
		return Literal.relation(TransactionMemoryTest.class, "book")
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
				.asserting(Collections.singletonList(person(null, lval(1), lval("Ada")))).get();
		assertThat(names(writer)).containsExactly("{Ada}");
		assertThat(writer.commit().isSuccess()).isTrue();

		try (Transaction reader = AbstractTransaction.over(store.open("r"))) {
			assertThat(names(reader)).containsExactly("{Ada}");
		}
	}

	private static SharedDatabase seededWithAda() {
		SharedDatabase store = SharedDatabase.empty();
		assertThat(AbstractTransaction.over(store.open("seed"))
				.asserting(Collections.singletonList(person(null, lval(1), lval("Ada"))))
				.get().commit().isSuccess()).isTrue();
		return store;
	}

	@Test
	public void aStagedRetractionShadowsOwnReadsAndAbandonmentLeavesNoTrace() throws Exception {
		SharedDatabase store = seededWithAda();
		Transaction tx = AbstractTransaction.over(store.open("tx"));
		assertThat(names(tx)).containsExactly("{Ada}");

		Transaction staged = tx.retracting(Collections.singletonList(
				person(null, lval(1), lval("Ada")))).get();
		assertThat(names(staged))
				.describedAs("the buffer's own reads stop seeing the staged removal")
				.isEmpty();

		try (Transaction reader = AbstractTransaction.over(store.open("r"))) {
			assertThat(names(reader))
					.describedAs("an abandoned retraction leaves no trace")
					.containsExactly("{Ada}");
		}
	}

	@Test
	public void aCommittedRetractionRemovesForTheNextTransaction() throws Exception {
		SharedDatabase store = seededWithAda();
		assertThat(AbstractTransaction.over(store.open("tx"))
				.retracting(Collections.singletonList(person(null, lval(1), lval("Ada"))))
				.get().commit().isSuccess()).isTrue();

		try (Transaction reader = AbstractTransaction.over(store.open("r"))) {
			assertThat(names(reader)).isEmpty();
		}
	}

	@Test
	public void aRetractionMovesTheRelationsWorld() throws Exception {
		SharedDatabase store = seededWithAda();
		Transaction reader = AbstractTransaction.over(store.open("reader"));
		assertThat(names(reader)).containsExactly("{Ada}");

		assertThat(AbstractTransaction.over(store.open("mover"))
				.retracting(Collections.singletonList(person(null, lval(1), lval("Ada"))))
				.get().commit().isSuccess()).isTrue();

		Try<?> refused = reader.asserting(Collections.singletonList(
				book(null, lval("i1"), lval("Tar Pit")))).get().commit();
		assertThat(refused.getCause())
				.describedAs("the pinned person region lost a row — the retraction divides pins")
				.isInstanceOf(Transaction.Conflict.class);
	}

	@Test
	public void assertingAndRetractingOneFactRefusesAsConflict() throws Exception {
		SharedDatabase store = seededWithAda();

		Try<Transaction> retractStaged = AbstractTransaction.over(store.open("a"))
				.asserting(Collections.singletonList(book(null, lval("i1"), lval("Tar Pit"))))
				.get()
				.retracting(Collections.singletonList(book(null, lval("i1"), lval("Tar Pit"))));
		assertThat(retractStaged.getCause())
				.describedAs("a transaction asserting and retracting one fact has not decided what it believes")
				.isInstanceOf(Transaction.Conflict.class);

		Try<Transaction> assertRemoved = AbstractTransaction.over(store.open("b"))
				.retracting(Collections.singletonList(person(null, lval(1), lval("Ada"))))
				.get()
				.asserting(Collections.singletonList(person(null, lval(1), lval("Ada"))));
		assertThat(assertRemoved.getCause())
				.isInstanceOf(Transaction.Conflict.class);
	}

	private static Literal loanOf(AnswerSource db, Unifiable<Integer> id, Unifiable<String> copy) {
		return Literal.relation(TransactionMemoryTest.class, "loan")
				.arg("loanId", id).indexed()
				.arg("copy", copy)
				.from(db);
	}

	private static Literal returnedOf(AnswerSource db, Unifiable<Integer> id) {
		return Literal.relation(TransactionMemoryTest.class, "returned")
				.arg("loanId", id).indexed()
				.from(db);
	}

	@Test
	public void aSelectedClusterRetractsThroughTheDoor() throws Exception {
		// the retract arc's face, composed instead of built: the question
		// selects the closed cluster, the door removes it, the commit
		// certifies the reads the closure stood on
		SharedDatabase store = SharedDatabase.empty();
		assertThat(AbstractTransaction.over(store.open("seed"))
				.asserting(loanOf(null, lval(1), lval("c1")), returnedOf(null, lval(1)))
				.get().commit().isSuccess()).isTrue();

		Simulated tx = AbstractTransaction.over(store.open("compact"));
		Unifiable<Integer> id = lvar();
		Unifiable<String> copy = lvar();
		List<Answer> cluster = new BreadthFirstScheduler<>(Question.select(
				loanOf(tx, id, copy).and(returnedOf(tx, id)),
				loanOf(null, id, copy), returnedOf(null, id))).get();
		assertThat(cluster).hasSize(2);

		assertThat(tx.retracting(cluster).get().commit().isSuccess()).isTrue();

		try (Transaction reader = AbstractTransaction.over(store.open("after"))) {
			Unifiable<String> c = lvar();
			assertThat(loanOf(reader, lvar(), c).solve(c).count()).isZero();
			Unifiable<Integer> r = lvar();
			assertThat(returnedOf(reader, r).solve(r).count()).isZero();
		}
	}

	@Test
	public void writeSkewOnOneRelationMeetsTheConflict() throws Exception {
		SharedDatabase store = SharedDatabase.empty();
		Transaction first = AbstractTransaction.over(store.open("first"));
		Transaction second = AbstractTransaction.over(store.open("second"));
		assertThat(names(first)).isEmpty();
		assertThat(names(second)).isEmpty();

		first = first.asserting(Collections.singletonList(
				person(null, lval(1), lval("Ada")))).get();
		second = second.asserting(Collections.singletonList(
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
						.asserting(Collections.singletonList(person(null, lval(1), lval("Ada")))).get()
						.asserting(Collections.singletonList(book(null, lval("978-0"), lval("SICP")))).get()
		) {
			assertThat(seed.commit().isSuccess()).isTrue();
		}

		Transaction bookWriter = AbstractTransaction.over(store.open("books"));
		Transaction personWriter = AbstractTransaction.over(store.open("people"));
		assertThat(titles(bookWriter)).containsExactly("{SICP}");
		assertThat(names(personWriter)).containsExactly("{Ada}");

		bookWriter = bookWriter.asserting(Collections.singletonList(
				book(null, lval("978-1"), lval("TAPL")))).get();
		personWriter = personWriter.asserting(Collections.singletonList(
				person(null, lval(2), lval("Alan")))).get();

		assertThat(bookWriter.commit().isSuccess()).isTrue();
		assertThat(personWriter.commit()
				.isSuccess())
				.describedAs("person marks never moved — the book commit must not bounce this one")
				.isTrue();
	}

	@Test
	public void aForeignDisjointCommitBetweenReadsDoesNotBounce() throws Exception {
		// the disjoint receipt's interleaved variant: the person region's
		// FIRST touch happens AFTER the book commit moved the world — the
		// pin carries the captured generations, the global fast path fails,
		// and the per-relation compare must still prove person unmoved
		SharedDatabase store = SharedDatabase.empty();
		Transaction people = AbstractTransaction.over(store.open("people"));

		try (
				Transaction books = AbstractTransaction.over(store.open("books"))
						.asserting(Collections.singletonList(book(null, lval("978-0"), lval("SICP")))).get()
		) {
			assertThat(books.commit().isSuccess()).isTrue();
		}

		assertThat(names(people)).isEmpty();
		Transaction staged = people.asserting(Collections.singletonList(
				person(null, lval(1), lval("Ada")))).get();
		assertThat(staged.commit()
				.isSuccess())
				.describedAs("only book moved — the disjoint person write must land")
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
						.asserting(Collections.singletonList(person(null, lval(1), lval("Ada")))).get()
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
						.asserting(Collections.singletonList(person(null, lval(1), lval("Ada")))).get()
		) {
			assertThat(names(abandoned)).containsExactly("{Ada}");
		}
		try (Transaction reader = AbstractTransaction.over(store.open("r"))) {
			assertThat(names(reader)).isEmpty();
		}
	}
}
