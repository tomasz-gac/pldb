package com.tgac.pldb.sql;

// ABOUTME: Region-grain receipts: disjoint regions of ONE relation commit across
// ABOUTME: each other, an insert into a pinned region bounces, no column refuses.

import static com.tgac.logic.finitedomain.FiniteDomain.dom;
import static com.tgac.logic.finitedomain.domains.EnumeratedDomain.range;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.transaction.AbstractTransaction;
import com.tgac.pldb.transaction.Transaction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VersionedWatermarkTest {

	private static final String URL = "jdbc:h2:mem:versioned;DB_CLOSE_DELAY=-1";

	private final List<Connection> opened = new ArrayList<>();

	@Before
	public void resetSchema() throws SQLException {
		try (
				Connection admin = DriverManager.getConnection(URL);
				Statement ddl = admin.createStatement()
		) {
			ddl.execute("DROP TABLE IF EXISTS loan");
			ddl.execute("DROP TABLE IF EXISTS book");
			ddl.execute("DROP TABLE IF EXISTS watermark");
			ddl.execute("CREATE TABLE loan(member VARCHAR(16) NOT NULL, copy VARCHAR(16) NOT NULL,"
					+ " version BIGINT NOT NULL)");
			ddl.execute("CREATE TABLE book(isbn VARCHAR(32) NOT NULL, title VARCHAR(64) NOT NULL)");
			ddl.execute("DROP TABLE IF EXISTS invoice");
			ddl.execute("CREATE TABLE invoice(member VARCHAR(16) NOT NULL, due BIGINT NOT NULL,"
					+ " version BIGINT NOT NULL)");
			Watermark.schema(ddl);
		}
	}

	@After
	public void closeConnections() {
		for (Connection connection : opened) {
			try {
				connection.close();
			} catch (SQLException ignored) {
				// closing what a test already closed is fine
			}
		}
	}

	private Connection connection() {
		try {
			Connection connection = DriverManager.getConnection(URL);
			opened.add(connection);
			return connection;
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	private Transaction transaction(String id) {
		return AbstractTransaction.over(
				VersionedWatermark.over(SqlFetch.live(id, connection()), this::connection));
	}

	private static Literal loan(AnswerSource db, Unifiable<String> member, Unifiable<String> copy) {
		return Literal.relation(VersionedWatermarkTest.class, "loan")
				.arg("member", member).indexed()
				.arg("copy", copy)
				.from(db);
	}

	private static Literal book(AnswerSource db, Unifiable<String> isbn, Unifiable<String> title) {
		return Literal.relation(VersionedWatermarkTest.class, "book")
				.arg("isbn", isbn).indexed()
				.arg("title", title)
				.from(db);
	}

	private static List<String> loansOf(AnswerSource db, String member) {
		Unifiable<String> copy = lvar();
		return loan(db, lval(member), copy).solve(copy)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	public void disjointRegionsOfOneRelationCommitAcrossEachOther() throws Exception {
		// the receipt the relation-grain Watermark cannot pass: both
		// transactions read and write LOAN, but different members' regions
		Transaction m1 = transaction("m1");
		Transaction m2 = transaction("m2");
		assertThat(loansOf(m1, "m1")).isEmpty();
		assertThat(loansOf(m2, "m2")).isEmpty();

		m2 = m2.withFacts(Collections.singletonList(loan(null, lval("m2"), lval("c2")))).get();
		assertThat(m2.commit().isSuccess()).isTrue();

		m1 = m1.withFacts(Collections.singletonList(loan(null, lval("m1"), lval("c1")))).get();
		assertThat(m1.commit()
				.isSuccess())
				.describedAs("m1's region never moved — m2's commit to the SAME relation must not bounce it")
				.isTrue();

		try (Transaction reader = transaction("reader")) {
			assertThat(loansOf(reader, "m1")).containsExactly("{c1}");
			assertThat(loansOf(reader, "m2")).containsExactly("{c2}");
		}
	}

	@Test
	public void anInsertIntoAPinnedEmptyRegionBounces() throws Exception {
		// the phantom case row-grain locking cannot see: no row this
		// transaction read changed — a row ARRIVED in its region
		Transaction reader = transaction("reader");
		assertThat(loansOf(reader, "m1")).isEmpty();

		try (
				Transaction mover = transaction("mover")
						.withFacts(Collections.singletonList(loan(null, lval("m1"), lval("c9")))).get()
		) {
			assertThat(mover.commit().isSuccess()).isTrue();
		}

		Transaction staged = reader.withFacts(Collections.singletonList(
				loan(null, lval("m1"), lval("c1")))).get();
		assertThat(staged.commit().getCause())
				.describedAs("the pinned empty region gained a row — the decision stood on its absence")
				.isInstanceOf(Transaction.Conflict.class);
	}

	private static Literal invoice(AnswerSource db, Unifiable<String> member, Unifiable<Long> day) {
		return Literal.relation(VersionedWatermarkTest.class, "invoice")
				.arg("member", member)
				.arg("due", day)
				.from(db);
	}

	/** Reads the FD-constrained region day ∈ [1,10]; the domain rides the probe. */
	private static List<Long> earlyInvoices(AnswerSource db) {
		Unifiable<Long> day = lvar();
		return dom(day, range(1L, 10L))
				.and(invoice(db, lvar(), day))
				.solve(day)
				.map(Term::get)
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	public void aCommitOutsideTheFdRegionDoesNotConflict() throws Exception {
		// the pushed domain narrows the PIN, not just the fetch: the pinned
		// region is "invoices with day in [1,10]", so a row landing at day 50
		// never moves its MAX — relation grain would have bounced this
		Transaction reader = transaction("fd-reader");
		assertThat(earlyInvoices(reader)).isEmpty();

		try (
				Transaction mover = transaction("mover")
						.withFacts(Collections.singletonList(invoice(null, lval("m9"), lval(50L)))).get()
		) {
			assertThat(mover.commit().isSuccess()).isTrue();
		}

		Transaction staged = reader.withFacts(Collections.singletonList(
				invoice(null, lval("a1"), lval(5L)))).get();
		assertThat(staged.commit()
				.isSuccess())
				.describedAs("day 50 lies outside the pinned domain — the FD region never moved")
				.isTrue();
	}

	@Test
	public void aCommitInsideTheFdRegionConflicts() throws Exception {
		Transaction reader = transaction("fd-reader");
		assertThat(earlyInvoices(reader)).isEmpty();

		try (
				Transaction mover = transaction("mover")
						.withFacts(Collections.singletonList(invoice(null, lval("m9"), lval(5L)))).get()
		) {
			assertThat(mover.commit().isSuccess()).isTrue();
		}

		Transaction staged = reader.withFacts(Collections.singletonList(
				invoice(null, lval("a1"), lval(7L)))).get();
		assertThat(staged.commit().getCause())
				.describedAs("day 5 lies inside the pinned domain — the FD region moved")
				.isInstanceOf(Transaction.Conflict.class);
	}

	@Test
	public void aTableWithoutTheVersionColumnRefusesLoudly() {
		Transaction transaction = transaction("books");
		Unifiable<String> title = lvar();
		assertThatThrownBy(() -> book(transaction, lval("978-0"), title).solve(title)
				.collect(Collectors.toList()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("book")
				.hasMessageContaining("version");
	}
}
