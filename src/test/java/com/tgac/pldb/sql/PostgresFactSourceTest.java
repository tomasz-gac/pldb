package com.tgac.pldb.sql;

// ABOUTME: The north star's Phase 2 proof against real PostgreSQL (testcontainers):
// ABOUTME: a nonrecursive and a recursive relation answer identically over memory and PG.

import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relations;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Phase 2's falsifiable result (docs/design/domain-layer.md §12): one
 * nonrecursive and one recursive relation produce the same answer set
 * over in-memory and PostgreSQL facts — the backing swaps under the
 * seam and nothing above it can tell. Skips cleanly when Docker is not
 * available.
 */
public class PostgresFactSourceTest {

	private static final Property<Integer> id = Property.of("id");
	private static final Property<String> name = Property.of("name");
	private static final Property<Integer> src = Property.of("src");
	private static final Property<Integer> dst = Property.of("dst");

	private static final Relations._2<Integer, String> person =
			Relations.relation("person", id.indexed(), name);
	private static final Relations._2<Integer, Integer> edge =
			Relations.relation("edge", src.indexed(), dst.indexed());

	private static final List<Fact> facts = Arrays.asList(
			person.fact(1, "Ada"),
			person.fact(2, "Alan"),
			person.fact(3, "Kurt"),
			edge.fact(1, 2),
			edge.fact(1, 3),
			edge.fact(2, 4),
			edge.fact(3, 4));

	private static final Database reference = ImmutableDatabase.empty()
			.withFacts(facts)
			.get();

	private static PostgreSQLContainer<?> postgres;

	private Connection connection;
	private SqlFactSource source;

	@BeforeClass
	public static void startPostgres() {
		Assume.assumeTrue("Docker unavailable — the Phase 2 proof needs it",
				DockerClientFactory.instance().isDockerAvailable());
		postgres = new PostgreSQLContainer<>("postgres:16-alpine");
		postgres.start();
	}

	@AfterClass
	public static void stopPostgres() {
		if (postgres != null) {
			postgres.stop();
		}
	}

	@Before
	public void loadPostgres() throws SQLException {
		connection = DriverManager.getConnection(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
		push(connection, facts);
		source = SqlFactSource.pinned("pg", connection);
	}

	/**
	 * The reference facts pushed into the backend by the naming convention
	 * the adapter reads by: the relation's name is the table, its property
	 * names the columns, column types inferred from the values. One fact
	 * list feeds both worlds — the proof compares backings, not fixtures.
	 */
	private static void push(Connection connection, List<Fact> facts) throws SQLException {
		Map<Relation, List<Fact>> byRelation = facts.stream()
				.collect(Collectors.groupingBy(Fact::getRelation,
						LinkedHashMap::new, Collectors.toList()));
		try (Statement ddl = connection.createStatement()) {
			for (Map.Entry<Relation, List<Fact>> table : byRelation.entrySet()) {
				ddl.execute("DROP TABLE IF EXISTS " + table.getKey().getName());
				ddl.execute(createTable(table.getKey(), table.getValue().get(0)));
			}
		}
		for (Map.Entry<Relation, List<Fact>> table : byRelation.entrySet()) {
			String placeholders = table.getValue().get(0).getValues().toJavaStream()
					.map(v -> "?")
					.collect(Collectors.joining(", "));
			try (
					PreparedStatement insert = connection.prepareStatement(
							"INSERT INTO " + table.getKey().getName() + " VALUES (" + placeholders + ")")
			) {
				for (Fact fact : table.getValue()) {
					int column = 1;
					for (Object value : fact.getValues()) {
						insert.setObject(column++, value);
					}
					insert.addBatch();
				}
				insert.executeBatch();
			}
		}
	}

	private static String createTable(Relation relation, Fact sample) {
		Property<?>[] columns = relation.getArgs();
		StringBuilder ddl = new StringBuilder("CREATE TABLE ")
				.append(relation.getName()).append("(");
		for (int i = 0; i < columns.length; i++) {
			ddl.append(i == 0 ? "" : ", ")
					.append(columns[i].getName())
					.append(" ").append(sqlType(sample.getValues().get(i)))
					.append(" NOT NULL");
		}
		return ddl.append(")").toString();
	}

	private static String sqlType(Object value) {
		if (value instanceof Integer) {
			return "INT";
		}
		if (value instanceof Long) {
			return "BIGINT";
		}
		if (value instanceof String) {
			return "VARCHAR(64)";
		}
		throw new IllegalArgumentException("no column type for " + value.getClass());
	}

	@After
	public void closePostgres() throws SQLException {
		source.close();
		if (!connection.isClosed()) {
			connection.close();
		}
	}

	/** The exact answers for {@code out}, rendered and sorted. */
	private static List<String> answers(Goal goal, Unifiable<?> out) {
		return goal.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	/**
	 * The closure of edge over the given backing — a fresh derived relation
	 * per source. Deliberately MIXED consumption: the base case must
	 * ENUMERATE (exists — the generator's choices live in the search tree),
	 * while the recursive conjunct rides posted as a GUARD — sound here
	 * because self's delivery grounds z first and every recursive step has a
	 * single successor, so the collapse fires and the guard discharges into
	 * ground answers. On data whose recursive steps branch, the guard would
	 * park and answers would go conditional.
	 */
	private static Literal reach(AnswerSource backing, Unifiable<Integer> x, Unifiable<Integer> y) {
		return Literal.relation("reachable").arg("src", x).arg("dst", y).solving(edge.exists(backing, x, y)
								.or(defer(() -> {
									Unifiable<Integer> z = lvar();
									return reach(backing, x, z)
											.and(edge.posted(backing, z, y));
								})));
	}

	@Test
	public void aNonrecursiveRelationAnswersIdentically() {
		Unifiable<Integer> viaPg = lvar();
		Unifiable<String> pgName = lvar();
		Unifiable<Integer> viaMemory = lvar();
		Unifiable<String> memoryName = lvar();
		List<String> pg = answers(person.exists(source, viaPg, pgName),
				lval(Tuple.of(viaPg, pgName)));
		List<String> memory = answers(person.exists(reference, viaMemory, memoryName),
				lval(Tuple.of(viaMemory, memoryName)));
		assertThat(pg).isNotEmpty().isEqualTo(memory);
	}

	@Test
	public void aPostedRelationAnswersIdentically() {
		Unifiable<Integer> viaPg = lvar();
		Unifiable<String> pgName = lvar();
		Unifiable<Integer> viaMemory = lvar();
		Unifiable<String> memoryName = lvar();
		List<String> pg = answers(person.posted(source, viaPg, pgName),
				lval(Tuple.of(viaPg, pgName)));
		List<String> memory = answers(person.posted(reference, viaMemory, memoryName),
				lval(Tuple.of(viaMemory, memoryName)));
		assertThat(pg).isNotEmpty().isEqualTo(memory);
	}

	@Test
	public void aRecursiveRelationAnswersIdentically() {
		// the diamond closes over PostgreSQL exactly as over memory: the
		// derived closure seals, 1 reaches 4 both ways, duplicates fold
		Unifiable<Integer> pgFrom = lvar();
		Unifiable<Integer> pgTo = lvar();
		Unifiable<Integer> memoryFrom = lvar();
		Unifiable<Integer> memoryTo = lvar();
		List<String> pg = answers(reach(source, pgFrom, pgTo).posted(),
				lval(Tuple.of(pgFrom, pgTo)));
		List<String> memory = answers(reach(reference, memoryFrom, memoryTo).posted(),
				lval(Tuple.of(memoryFrom, memoryTo)));
		assertThat(pg).isNotEmpty().isEqualTo(memory);
	}
}
