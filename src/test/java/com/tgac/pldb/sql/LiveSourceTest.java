package com.tgac.pldb.sql;

// ABOUTME: The live lane's receipts: the world moves between reads, the cache
// ABOUTME: keeps a solve's view still, and a torn transaction meets the Conflict.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.tabling.Call;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.transaction.AbstractTransaction;
import com.tgac.pldb.transaction.Transaction;
import io.vavr.collection.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LiveSourceTest {

	private static final String URL = "jdbc:h2:mem:livetest;DB_CLOSE_DELAY=-1";

	private final List<Connection> opened = new ArrayList<>();

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

	@Before
	public void resetSchema() throws SQLException {
		try (
				Connection admin = DriverManager.getConnection(URL);
				Statement ddl = admin.createStatement()
		) {
			ddl.execute("DROP TABLE IF EXISTS person");
			ddl.execute("DROP TABLE IF EXISTS book");
			ddl.execute("DROP TABLE IF EXISTS watermark");
			ddl.execute("CREATE TABLE person(id INT NOT NULL, name VARCHAR(64) NOT NULL)");
			ddl.execute("CREATE TABLE book(isbn VARCHAR(32) NOT NULL, title VARCHAR(64) NOT NULL)");
			Watermark.schema(ddl);
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

	private static Call<Relation> probe(Literal shape) {
		Term<?>[] anys = new Term<?>[shape.getRel().getArgs().length];
		for (int i = 0; i < anys.length; i++) {
			anys[i] = Any.of(i);
		}
		return Call.of(shape.getRel(), (Reified<?>) lval(Array.of(anys)));
	}

	private static List<String> names(Iterable<Answer> answers) {
		return StreamSupport.stream(answers.spliterator(), false)
				.map(answer -> answer.getReified().toString())
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	public void theLiveWorldMovesBetweenTwoReadsOfOneSource() throws Exception {
		SqlFetch live = SqlFetch.live("live", connection());
		Call<Relation> people = probe(person(null, lvar(), lvar()));
		assertThat(live.answers(people)).isEmpty();

		try (Statement writer = connection().createStatement()) {
			writer.execute("INSERT INTO person VALUES (1, 'Ada')");
		}

		// the same source, the same probe, a DIFFERENT world: no snapshot
		// stands between the reads — this is the medium the pinned lane
		// deliberately refuses to be
		assertThat(names(live.answers(people))).containsExactly("{Array({1}, {Ada})}");
	}

	@Test
	public void theCachingLedgerKeepsASolveViewStill() throws Exception {
		try (CachingSqlFetch source = CachingSqlFetch.live("cached-live", connection())) {
			Unifiable<String> name = lvar();
			assertThat(person(source, lvar(), name).solve(name)
					.map(Object::toString)
					.collect(Collectors.toList())).isEmpty();

			try (Statement writer = connection().createStatement()) {
				writer.execute("INSERT INTO person VALUES (1, 'Ada')");
			}

			// the world moved, the covered probe did not: repeats serve from
			// the ledger — read stability is the CACHE's job on this lane
			assertThat(person(source, lvar(), name).solve(name)
					.map(Object::toString)
					.collect(Collectors.toList())).isEmpty();
		}
	}

	@Test
	public void aTornTransactionMeetsTheConflict() throws Exception {
		Transaction torn = transaction("torn");
		assertThat(names(torn.answers(probe(person(null, lvar(), lvar()))))).isEmpty();

		try (
				Transaction mover = transaction("mover")
						.withFacts(Collections.singletonList(person(null, lval(1), lval("Ada")))).get()
		) {
			assertThat(mover.commit().isSuccess()).isTrue();
		}

		// book reads the CURRENT world while person was pinned before the
		// move: the transaction's own view is torn across two worlds, and
		// the commit proof must refuse it
		assertThat(names(torn.answers(probe(book(null, lvar(), lvar()))))).isEmpty();
		Transaction staged = torn.withFacts(Collections.singletonList(
				book(null, lval("978-0"), lval("SICP")))).get();
		assertThat(staged.commit().getCause())
				.describedAs("person moved after its pin — a torn view must not land")
				.isInstanceOf(Transaction.Conflict.class);
	}

	private Transaction transaction(String id) {
		return AbstractTransaction.over(
				Watermark.over(CachingSqlFetch.live(id, connection()), this::connection));
	}
}
