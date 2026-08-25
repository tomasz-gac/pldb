package com.tgac.pldb.sql;

// ABOUTME: Pins the SQL adapter: an H2-backed FactSource answers identically to
// ABOUTME: the in-memory reference, refuses unmapped relations, and lands fetches
// ABOUTME: so subsumed probes never touch the backend again.

import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import com.tgac.pldb.FactSource;
import com.tgac.pldb.ImmutableDatabase;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SqlFactSourceTest {

	private static final Property<Integer> id = Property.of("id");
	private static final Property<String> name = Property.of("name");

	private static final Relations._2<Integer, String> person =
			Relations.relation("person", id.indexed(), name);

	private static final Database reference = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					person.fact(1, "Ada"),
					person.fact(2, "Alan"),
					person.fact(3, "Kurt")))
			.get();

	private Connection connection;
	private final AtomicInteger statements = new AtomicInteger();

	@Before
	public void loadH2() throws SQLException {
		connection = DriverManager.getConnection("jdbc:h2:mem:");
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE person(id INT, name VARCHAR(64))");
			ddl.execute("INSERT INTO person VALUES (1, 'Ada'), (2, 'Alan'), (3, 'Kurt')");
		}
	}

	@After
	public void closeH2() throws SQLException {
		connection.close();
	}

	private SqlFactSource source() {
		return SqlFactSource.pinned("h2-test", counting(connection),
				SqlMapping.of(person, "person",
						SqlColumn.of("id"), SqlColumn.of("name")));
	}

	@Test
	public void answersLikeTheInMemoryReference() {
		try (SqlFactSource source = source()) {
			assertThat(solvedNames(source)).isEqualTo(solvedNames(reference));
		}
	}

	@Test
	public void aPostedConstraintAnswersLikeTheInMemoryReference() {
		try (SqlFactSource source = source()) {
			Unifiable<Integer> viaSql = lvar();
			Unifiable<Integer> viaDb = lvar();
			assertThat(person.posted(source, viaSql, lvar())
					.solve(viaSql)
					.map(Object::toString)
					.collect(Collectors.toList()))
					.isEqualTo(person.posted(reference, viaDb, lvar())
							.solve(viaDb)
							.map(Object::toString)
							.collect(Collectors.toList()));
		}
	}

	@Test
	public void anUnmappedRelationRefusesLoudly() {
		Relations._1<Integer> orphan = Relations.relation("orphan", id);
		try (SqlFactSource source = source()) {
			assertThatThrownBy(() -> orphan.exists(source, lvar()).solve(lvar()).count())
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("orphan");
		}
	}

	@Test
	public void aSubsumedProbeIsServedFromTheLandedPoolWithoutAFetch() {
		try (SqlFactSource source = source()) {
			solvedNames(source);                       // the wide fetch: nothing bound
			int afterWide = statements.get();

			Unifiable<String> narrow = lvar();
			List<String> viaLanded = person.exists(source, com.tgac.logic.unification.LVal.lval(2), narrow)
					.solve(narrow)
					.map(Object::toString)
					.collect(Collectors.toList());

			assertThat(viaLanded).hasSize(1);
			assertThat(viaLanded.get(0)).contains("Alan");
			assertThat(statements.get())
					.describedAs("a probe subsumed by a covered one must not touch the backend")
					.isEqualTo(afterWide);
		}
	}

	private static List<String> solvedNames(FactSource source) {
		Unifiable<String> out = lvar();
		return person.exists(source, lvar(), out)
				.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	/** Counts prepared statements, so the landing receipts can see fetches. */
	private Connection counting(Connection real) {
		return (Connection) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[]{Connection.class},
				(proxy, method, args) -> {
					if ("prepareStatement".equals(method.getName())) {
						statements.incrementAndGet();
					}
					try {
						return method.invoke(real, args);
					} catch (java.lang.reflect.InvocationTargetException e) {
						throw e.getCause();
					}
				});
	}
}
