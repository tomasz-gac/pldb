package com.tgac.pldb.transaction;

// ABOUTME: The Transaction over the owned certify tier: watermark receipts on H2 —
// ABOUTME: refusal without a capability, write skew refused, disjoint relations pass.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.sql.Watermark;
import com.tgac.pldb.sql.CachingSqlFetch;
import io.vavr.control.Try;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class TransactionTest {

	private static final String URL = "jdbc:h2:mem:txtest;DB_CLOSE_DELAY=-1";

	@Before
	public void resetSchema() throws SQLException {
		try (Connection admin = DriverManager.getConnection(URL);
				Statement ddl = admin.createStatement()) {
			ddl.execute("DROP TABLE IF EXISTS person");
			ddl.execute("DROP TABLE IF EXISTS book");
			ddl.execute("DROP TABLE IF EXISTS watermark");
			ddl.execute("CREATE TABLE person(id INT NOT NULL, name VARCHAR(64) NOT NULL)");
			ddl.execute("CREATE TABLE book(isbn VARCHAR(32) NOT NULL, title VARCHAR(64) NOT NULL)");
			Watermark.schema(ddl);
		}
	}

	private static Transaction transaction(String id) throws Exception {
		Connection connection = DriverManager.getConnection(URL);
		return AbstractTransaction.over(Watermark.over(CachingSqlFetch.pinned(id, connection),
						TransactionTest::commitConnection));
	}

	private static Connection commitConnection() {
		try {
			return DriverManager.getConnection(URL);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

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

	// a source without a certify capability has no Transaction.over overload to
	// call — the refusal moved from runtime into the type system, so its old
	// receipt (aSourceWithoutACertifyCapabilityRefusesAtOpen) is now javac's job

	@Test
	public void commitLandsStagedFactsForTheNextTransaction() throws Exception {
		Transaction writer = transaction("w")
				.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada")).fact())).get();
		assertThat(names(writer)).containsExactly("{Ada}");
		assertThat(writer.commit().isSuccess()).isTrue();

		try (Transaction reader = transaction("r")) {
			assertThat(names(reader)).containsExactly("{Ada}");
		}
	}

	@Test
	public void writeSkewOnOneRelationMeetsTheConflict() throws Exception {
		// both transactions read the person region their sibling writes: the
		// first commit wins, the second's watermark moved — Conflict, re-solve
		Transaction first = transaction("first");
		Transaction second = transaction("second");
		assertThat(names(first)).isEmpty();
		assertThat(names(second)).isEmpty();

		first = first.withFacts(Collections.singletonList(
				person(null, lval(1), lval("Ada")).fact())).get();
		second = second.withFacts(Collections.singletonList(
				person(null, lval(2), lval("Alan")).fact())).get();

		assertThat(first.commit().isSuccess()).isTrue();
		Try<?> refused = second.commit();
		assertThat(refused.isFailure()).isTrue();
		assertThat(refused.getCause()).isInstanceOf(Transaction.Conflict.class);

		try (Transaction reader = transaction("r")) {
			assertThat(names(reader)).containsExactly("{Ada}");
		}
	}

	@Test
	public void disjointRelationsCommitWithoutConflict() throws Exception {
		// the per-relation marks earn their keep: one transaction read only
		// person, the other committed only book — no conflict between them
		try (Transaction seed = transaction("seed")
				.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada")).fact())).get()
				.withFacts(Collections.singletonList(book(null, lval("978-0"), lval("SICP")).fact())).get()) {
			assertThat(seed.commit().isSuccess()).isTrue();
		}

		Transaction bookWriter = transaction("books");
		Transaction personWriter = transaction("people");
		assertThat(titles(bookWriter)).containsExactly("{SICP}");
		assertThat(names(personWriter)).containsExactly("{Ada}");

		bookWriter = bookWriter.withFacts(Collections.singletonList(
				book(null, lval("978-1"), lval("TAPL")).fact())).get();
		personWriter = personWriter.withFacts(Collections.singletonList(
				person(null, lval(2), lval("Alan")).fact())).get();

		assertThat(bookWriter.commit().isSuccess()).isTrue();
		assertThat(personWriter.commit()
				.isSuccess())
				.describedAs("person marks never moved — the book commit must not bounce this one")
				.isTrue();
	}

	@Test
	public void anAbandonedTransactionLeavesNoTrace() throws Exception {
		try (Transaction abandoned = transaction("a")
				.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada")).fact())).get()) {
			assertThat(names(abandoned)).containsExactly("{Ada}");
		}
		try (Transaction reader = transaction("r")) {
			assertThat(names(reader)).isEmpty();
		}
	}
}
