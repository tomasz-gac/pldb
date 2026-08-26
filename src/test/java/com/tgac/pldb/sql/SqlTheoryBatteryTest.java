package com.tgac.pldb.sql;

// ABOUTME: The end-to-end theory battery: each constraint theory solved three ways
// ABOUTME: — pushed SQL, unpushed SQL, in-memory reference — and all must agree.

import static com.tgac.logic.finitedomain.FiniteDomain.dom;
import static com.tgac.logic.finitedomain.FiniteDomain.gtr;
import static com.tgac.logic.finitedomain.FiniteDomain.lss;
import static com.tgac.logic.finitedomain.FiniteDomain.separate;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.finitedomain.FiniteDomain;
import com.tgac.logic.finitedomain.domains.EnumeratedDomain;
import com.tgac.logic.finitedomain.domains.Interval;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import com.tgac.pldb.FactSource;
import com.tgac.pldb.ImmutableDatabase;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The oracle is the in-memory reference: for every theory shape the compiler
 * claims to push, the pushed SQL source, the unpushed SQL source, and the
 * reference database must produce identical answer lists over the same
 * program — and every battery entry asserts non-vacuity, so a theory that
 * silently stopped matching rows cannot pass.
 */
public class SqlTheoryBatteryTest {

	private static final Property<Long> id = Property.of("id");
	private static final Property<String> name = Property.of("name");
	private static final Relations._2<Long, String> person =
			Relations.relation("person", id.indexed(), name);

	private static final Property<Long> lo = Property.of("lo");
	private static final Property<Long> hi = Property.of("hi");
	private static final Relations._2<Long, Long> edge =
			Relations.relation("edge", lo.indexed(), hi.indexed());

	private static final Database reference = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					person.fact(1L, "Ada"),
					person.fact(2L, "Alan"),
					person.fact(3L, "Kurt"),
					person.fact(4L, "Barbara"),
					person.fact(5L, "Edsger"),
					edge.fact(1L, 2L),
					edge.fact(2L, 1L),
					edge.fact(3L, 3L),
					edge.fact(1L, 5L)))
			.get();

	private Connection connection;

	@Before
	public void loadH2() throws SQLException {
		connection = DriverManager.getConnection("jdbc:h2:mem:");
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE person(id BIGINT, name VARCHAR(64))");
			ddl.execute("INSERT INTO person VALUES " +
					StreamSupport.stream(reference.get(person, Array.of(Optional.empty(), Optional.empty()))
									.spliterator(), false)
							.map(f -> "(" + f.get(id).get() + ", '" + f.get(name).get() + "') ")
							.collect(Collectors.joining(",")));
			ddl.execute("CREATE TABLE edge(lo BIGINT, hi BIGINT)");
			ddl.execute("INSERT INTO edge VALUES " +
					StreamSupport.stream(reference.get(edge, Array.of(Optional.empty(), Optional.empty())).spliterator(), false)
							.map(f -> "(" + f.get(lo).get() + ", " + f.get(hi).get() + ") ")
							.collect(Collectors.joining(",")));
		}
	}

	@After
	public void closeH2() throws SQLException {
		connection.close();
	}

	@Test
	public void intervalDomain() {
		agree(2, (FactSource source, Unifiable<String> out) -> {
			Unifiable<Long> x = lvar();
			return dom(x, Interval.of(2L, 3L)).and(person.exists(source, x, out));
		});
	}

	@Test
	public void singletonDomainCollapsesToABoundProbe() {
		agree(1, (FactSource source, Unifiable<String> out) -> {
			Unifiable<Long> x = lvar();
			return dom(x, EnumeratedDomain.range(2L, 3L)).and(person.exists(source, x, out));
		});
	}

	@Test
	public void holeyDomainThroughTheSolveIsAUnion() {
		// separate punches the hole DURING propagation: the probe's region
		// carries a real Union, pushed as its members disjoined — [1,5] minus
		// {3} keeps four
		agree(4, (FactSource source, Unifiable<String> out) -> {
			Unifiable<Long> x = lvar();
			return dom(x, Interval.of(1L, 5L))
					.and(separate(x, lval(3L)))
					.and(person.exists(source, x, out));
		});
	}

	@Test
	public void strictOrderAgainstAValue() {
		agree(2, (FactSource source, Unifiable<String> out) -> {
			Unifiable<Long> x = lvar();
			return lss(x, lval(3L)).and(person.exists(source, x, out));
		});
	}

	@Test
	public void flippedStrictOrderAgainstAValue() {
		agree(2, (FactSource source, Unifiable<String> out) -> {
			Unifiable<Long> x = lvar();
			return gtr(x, lval(3L)).and(person.exists(source, x, out));
		});
	}

	@Test
	public void looseOrderAgainstAValue() {
		agree(3, (FactSource source, Unifiable<String> out) -> {
			Unifiable<Long> x = lvar();
			return FiniteDomain.leq(x, lval(3L)).and(person.exists(source, x, out));
		});
	}

	@Test
	public void disequalityAgainstAValue() {
		agree(4, (FactSource source, Unifiable<String> out) -> {
			Unifiable<Long> x = lvar();
			return separate(x, lval(3L)).and(person.exists(source, x, out));
		});
	}

	@Test
	public void strictOrderAcrossTwoColumns() {
		agree(2, (FactSource source, Unifiable<Long> out) -> {
			Unifiable<Long> b = lvar();
			return lss(out, b).and(edge.exists(source, out, b));
		});
	}

	@Test
	public void looseOrderAcrossTwoColumns() {
		agree(3, (FactSource source, Unifiable<Long> out) -> {
			Unifiable<Long> b = lvar();
			return FiniteDomain.leq(out, b).and(edge.exists(source, out, b));
		});
	}

	@Test
	public void disequalityAcrossTwoColumns() {
		agree(3, (FactSource source, Unifiable<Long> out) -> {
			Unifiable<Long> b = lvar();
			return separate(out, b).and(edge.exists(source, out, b));
		});
	}

	@Test
	public void aConjoinedTheoryPushesAllItsAtoms() {
		agree(1, (FactSource source, Unifiable<String> out) -> {
			Unifiable<Long> x = lvar();
			return dom(x, Interval.of(2L, 5L))
					.and(lss(x, lval(4L)))
					.and(separate(x, lval(2L)))
					.and(person.exists(source, x, out));
		});
	}

	private <T> void agree(int expected, BiFunction<FactSource, Unifiable<T>, Goal> program) {
		Tuple2<FactSource, FactSource> sources = Tuple.of(
				SqlFactSource.pinned("battery-push", connection),
				CachingFactSource.over(SqlFetch.pinned("battery-plain", connection)));
		List<String> pushed = answers(sources._1, program);
		List<String> unpushed = answers(sources._2, program);
		List<String> inMemory = answers(reference, program);
		assertThat(pushed).isEqualTo(inMemory);
		assertThat(unpushed).isEqualTo(inMemory);
		assertThat(pushed).hasSize(expected);
	}

	private static <T> List<String> answers(FactSource source, BiFunction<FactSource, Unifiable<T>, Goal> program) {
		Unifiable<T> out = lvar();
		return program.apply(source, out)
				.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}
}
