package com.tgac.pldb.sql;

// ABOUTME: The codec registry: builtins pass through, registered types round-trip
// ABOUTME: through flush, decode and bound probes; the unknown refuses by column.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CodecTest {

	private static final Codec<LocalDate> DATES =
			Codec.of(LocalDate.class, Date.class, Date::valueOf, Date::toLocalDate);

	private Connection connection;

	@Before
	public void openDatabase() throws SQLException {
		connection = DriverManager.getConnection("jdbc:h2:mem:");
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE loan(id INT NOT NULL, due DATE NOT NULL)");
		}
	}

	@After
	public void closeDatabase() throws SQLException {
		connection.close();
	}

	private static Literal loan(AnswerSource db, Unifiable<Integer> id, Unifiable<LocalDate> due) {
		return Literal.relation("loan")
				.arg("id", id).indexed()
				.arg("due", due)
				.from(db);
	}

	@Test
	public void aRegisteredTypeRoundTripsThroughFlushAndFetch() throws Exception {
		Codecs codecs = Codecs.builtin().codec(DATES);
		SqlFlush.over(connection, codecs).flush(Arrays.asList(
				loan(null, lval(1), lval(LocalDate.of(2026, 9, 11))),
				loan(null, lval(2), lval(LocalDate.of(2026, 12, 24)))));

		try (CachingSqlFetch source = CachingSqlFetch.pinned("h2", connection).codec(DATES)) {
			Unifiable<LocalDate> due = lvar();
			List<LocalDate> dues = loan(source, lvar(), due).solve(due)
					.map(reified -> reified.get())
					.sorted()
					.collect(Collectors.toList());
			assertThat(dues).containsExactly(
					LocalDate.of(2026, 9, 11), LocalDate.of(2026, 12, 24));
		}
	}

	@Test
	public void aBoundProbeEncodesItsParameter() throws Exception {
		Codecs codecs = Codecs.builtin().codec(DATES);
		SqlFlush.over(connection, codecs).flush(Arrays.asList(
				loan(null, lval(1), lval(LocalDate.of(2026, 9, 11))),
				loan(null, lval(2), lval(LocalDate.of(2026, 12, 24)))));

		try (CachingSqlFetch source = CachingSqlFetch.pinned("h2", connection).codec(DATES)) {
			Unifiable<Integer> id = lvar();
			assertThat(loan(source, id, lval(LocalDate.of(2026, 12, 24))).solve(id)
					.map(Object::toString)
					.collect(Collectors.toList())).containsExactly("{2}");
		}
	}

	@Test
	public void anUnregisteredTypeRefusesAtTheFlushByColumn() {
		assertThatThrownBy(() -> SqlFlush.over(connection).flush(Arrays.asList(
				loan(null, lval(1), lval(LocalDate.of(2026, 9, 11))))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("loan")
				.hasMessageContaining("due")
				.hasMessageContaining("LocalDate");
	}

	@Test
	public void aStructuralValueKeepsTheModellingRefusal() {
		Literal bad = Literal.relation("loan")
				.arg("id", lval(1)).indexed()
				.arg("due", lval((Object) Arrays.asList(1, 2)))
				.from(null);
		assertThatThrownBy(() -> SqlFlush.over(connection).flush(Arrays.asList(bad)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("child relation");
	}

	@Test
	public void registrationAfterFirstUseRefuses() throws Exception {
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE person(id INT NOT NULL, name VARCHAR(16) NOT NULL)");
			ddl.execute("INSERT INTO person VALUES (1, 'Ada')");
		}
		try (CachingSqlFetch source = CachingSqlFetch.pinned("h2", connection)) {
			Unifiable<String> name = lvar();
			Literal.relation("person").arg("id", lvar()).indexed().arg("name", name).from(source)
					.solve(name).collect(Collectors.toList());
			assertThatThrownBy(() -> source.codec(DATES))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("before first use");
		}
	}
}
