package com.tgac.pldb.sql;

// ABOUTME: The north star's Phase 2 proof against real PostgreSQL (testcontainers):
// ABOUTME: a nonrecursive and a recursive relation answer identically over memory and PG.

import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.tabling.Tabled;
import com.tgac.logic.tabling.Tabling;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
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

	private static final Database reference = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					person.fact(1, "Ada"),
					person.fact(2, "Alan"),
					person.fact(3, "Kurt"),
					edge.fact(1, 2),
					edge.fact(1, 3),
					edge.fact(2, 4),
					edge.fact(3, 4)))
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
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("DROP TABLE IF EXISTS person");
			ddl.execute("DROP TABLE IF EXISTS edge");
			ddl.execute("CREATE TABLE person(id INT NOT NULL, name VARCHAR(64) NOT NULL)");
			ddl.execute("CREATE TABLE edge(src INT NOT NULL, dst INT NOT NULL)");
			ddl.execute("INSERT INTO person VALUES (1, 'Ada'), (2, 'Alan'), (3, 'Kurt')");
			ddl.execute("INSERT INTO edge VALUES (1, 2), (1, 3), (2, 4), (3, 4)");
		}
		source = SqlFactSource.pinned("pg", connection);
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

	/** The closure of edge over the given backing — a fresh derived relation per source. */
	private static Relations._2<Integer, Integer>.Derived reach(AnswerSource backing) {
		Relations._2<Integer, Integer> reachable =
				Relations.relation("reachable", src, dst);
		Tabled<Tuple2<Unifiable<Integer>, Unifiable<Integer>>> path =
				Tabling.defineRecursive(self -> pair -> pair.apply((x, y) ->
						edge.exists(backing, x, y)
								.or(defer(() -> {
									Unifiable<Integer> z = lvar();
									return self.apply(Tuple.of(x, z))
											.and(edge.exists(backing, z, y));
								}))));
		return reachable.solving((x, y) -> path.apply(Tuple.of(x, y)));
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
		List<String> pg = answers(reach(source).exists(pgFrom, pgTo),
				lval(Tuple.of(pgFrom, pgTo)));
		List<String> memory = answers(reach(reference).exists(memoryFrom, memoryTo),
				lval(Tuple.of(memoryFrom, memoryTo)));
		assertThat(pg).isNotEmpty().isEqualTo(memory);
	}
}
