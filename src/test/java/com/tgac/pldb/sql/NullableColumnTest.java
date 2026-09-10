package com.tgac.pldb.sql;

// ABOUTME: Declared nullability: a nullable column reads NULL as the sentinel
// ABOUTME: value, writes it back as SQL NULL, and matches it via IS NULL.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Null;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NullableColumnTest {

	private Connection connection;

	@Before
	public void openDatabase() throws SQLException {
		connection = DriverManager.getConnection("jdbc:h2:mem:");
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE person(id INT NOT NULL, name VARCHAR(64))");
			ddl.execute("INSERT INTO person VALUES (1, 'Ada'), (2, NULL)");
		}
	}

	@After
	public void closeDatabase() throws SQLException {
		connection.close();
	}

	/** name declared nullable: the customer's schema allows it, so do we. */
	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<Object> name) {
		return Literal.relation("person")
				.arg("id", id).indexed()
				.arg("name", name).nullable()
				.from(db);
	}

	@Test
	public void aNullCellReadsAsTheSentinel() throws Exception {
		try (CachingSqlFetch source = CachingSqlFetch.pinned("h2", connection)) {
			Unifiable<Object> name = lvar();
			List<String> names = person(source, lvar(), name).solve(name)
					.map(Object::toString)
					.sorted()
					.collect(Collectors.toList());
			assertThat(names).containsExactly("{Ada}", "{NULL}");
		}
	}

	@Test
	public void aBoundSentinelMatchesOnlyNullRows() throws Exception {
		try (CachingSqlFetch source = CachingSqlFetch.pinned("h2", connection)) {
			Unifiable<Integer> id = lvar();
			List<String> ids = person(source, id, lval(Null.VALUE)).solve(id)
					.map(Object::toString)
					.collect(Collectors.toList());
			assertThat(ids).containsExactly("{2}");
		}
	}

	@Test
	public void theSentinelRoundTripsThroughTheFlush() throws Exception {
		SqlFlush.over(connection).flush(Arrays.asList(
				person(null, lval(3), lval(Null.VALUE)).fact()));
		try (Statement read = connection.createStatement()) {
			assertThat(read.executeQuery("SELECT COUNT(*) FROM person WHERE id = 3 AND name IS NULL")
					.next()).isTrue();
		}
		try (CachingSqlFetch source = CachingSqlFetch.pinned("h2", connection)) {
			Unifiable<Integer> id = lvar();
			assertThat(person(source, id, lval(Null.VALUE)).solve(id)
					.map(Object::toString)
					.sorted()
					.collect(Collectors.toList())).containsExactly("{2}", "{3}");
		}
	}

	/** The catcher stays for columns NOT declared nullable. */
	private static Literal strict(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation("person")
				.arg("id", id).indexed()
				.arg("name", name)
				.from(db);
	}

	@Test
	public void anUndeclaredColumnStillRefusesNull() throws Exception {
		try (CachingSqlFetch source = CachingSqlFetch.pinned("h2", connection)) {
			Unifiable<String> name = lvar();
			assertThatThrownBy(() -> strict(source, lvar(), name).solve(name)
					.collect(Collectors.toList()))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("name");
		}
	}

	@Test
	public void theSentinelOnAStrictColumnRefusesAtTheFlush() {
		Literal sentinelOnStrict = Literal.relation("person")
				.arg("id", lval(4)).indexed()
				.arg("name", lval(Null.VALUE))
				.from(null);
		assertThatThrownBy(() -> SqlFlush.over(connection).flush(Arrays.asList(
				sentinelOnStrict.fact())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("person")
				.hasMessageContaining("name");
	}
}
