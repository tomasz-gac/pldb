package com.tgac.pldb.sql;

// ABOUTME: The write face: facts land as INSERTs by the schema convention, and a
// ABOUTME: structural column value refuses by relation and column before any row lands.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SqlFlushTest {

	private Connection connection;

	@Before
	public void openDatabase() throws SQLException {
		connection = DriverManager.getConnection("jdbc:h2:mem:");
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE person(id INT, name VARCHAR(64))");
			ddl.execute("CREATE TABLE visited(city VARCHAR(64))");
		}
	}

	@After
	public void closeDatabase() throws SQLException {
		connection.close();
	}

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation(SqlFlushTest.class, "person").arg("id", id).indexed().arg("name", name).from(db);
	}

	private static Literal visited(AnswerSource db, Unifiable<Object> city) {
		return Literal.relation(SqlFlushTest.class, "visited").arg("city", city).from(db);
	}

	private List<String> namesReadBack() throws Exception {
		try (CachingSqlFetch source = CachingSqlFetch.pinned("h2", connection)) {
			Unifiable<String> name = lvar();
			return person(source, lvar(), name).solve(name)
					.map(Object::toString)
					.sorted()
					.collect(Collectors.toList());
		}
	}

	@Test
	public void flushedFactsComeBackThroughTheFetch() throws Exception {
		SqlFlush.over(connection).flush(Arrays.asList(
				person(null, lval(1), lval("Ada")),
				person(null, lval(2), lval("Alan"))));
		assertThat(namesReadBack()).containsExactly("{Ada}", "{Alan}");
	}

	@Test
	public void oneFlushLandsInEveryTableItNames() throws SQLException {
		SqlFlush.over(connection).flush(Arrays.asList(
				person(null, lval(1), lval("Ada")),
				visited(null, lval((Object) "Zurich"))));
		try (
				Statement read = connection.createStatement();
				ResultSet rows = read.executeQuery("SELECT city FROM visited")
		) {
			assertThat(rows.next()).isTrue();
			assertThat(rows.getString(1)).isEqualTo("Zurich");
		}
	}

	@Test
	public void aStructuralColumnValueRefusesByNameBeforeAnyRowLands() throws Exception {
		Literal structural = visited(null, lval((Object) Arrays.asList("Zurich", "Bern")));
		assertThatThrownBy(() -> SqlFlush.over(connection).flush(Arrays.asList(
				person(null, lval(1), lval("Ada")),
				structural)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("visited")
				.hasMessageContaining("city");
		assertThat(namesReadBack())
				.describedAs("the refusal precedes every insert of the batch")
				.isEmpty();
	}

	@Test
	public void anEmptyFlushIsANoOp() throws Exception {
		SqlFlush.over(connection).flush(Collections.<com.tgac.pldb.relations.Answer> emptyList());
		assertThat(namesReadBack()).isEmpty();
	}
}
