package com.tgac.pldb.sql;

// ABOUTME: Pins the pushdown core: the coverage trap (a pushed fetch must not serve
// ABOUTME: a wider probe), answer identity vs the unpushed source, locality receipts.

import static com.tgac.logic.finitedomain.FiniteDomain.dom;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.finitedomain.FiniteDomain;
import com.tgac.logic.finitedomain.FiniteDomainConstraints;
import com.tgac.logic.finitedomain.domains.EnumeratedDomain;
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

public class SqlPushdownTest {

	private static final Property<Long> id = Property.of("id");
	private static final Property<String> name = Property.of("name");

	private static final Relations._2<Long, String> person =
			Relations.relation("person", id.indexed(), name);

	private static final Database reference = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					person.fact(1L, "Ada"),
					person.fact(2L, "Alan"),
					person.fact(3L, "Kurt")))
			.get();

	private Connection connection;
	private final AtomicInteger statements = new AtomicInteger();
	private final List<String> statementSql = new java.util.ArrayList<>();

	@Before
	public void loadH2() throws SQLException {
		connection = DriverManager.getConnection("jdbc:h2:mem:");
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE person(id BIGINT, name VARCHAR(64))");
			ddl.execute("INSERT INTO person VALUES (1, 'Ada'), (2, 'Alan'), (3, 'Kurt')");
		}
	}

	@After
	public void closeH2() throws SQLException {
		connection.close();
	}

	/** Sources share the test's connection; the @After close owns its lifecycle. */
	private SqlFactSource pushing() {
		return SqlFactSource.pinned("h2-push", counting(connection));
	}

	/** The unpushed leg of the oracle: the bare halves composed, no equipment. */
	private FactSource plain() {
		return CachingFactSource.over(SqlFetch.pinned("h2-plain", counting(connection)));
	}

	@Test
	public void aPushedFetchMustNotServeAWiderProbe() {
		// THE TRAP: the narrow fetch lands rows 1..2 under its pushed WHERE;
		// coverage recorded by pattern alone would claim completeness for the
		// whole relation and silently lose Kurt
		SqlFactSource source = pushing();
		Unifiable<Long> narrow = lvar();
		assertThat(dom(narrow, EnumeratedDomain.range(1L, 3L))
				.and(person.exists(source, narrow, lvar()))
				.solve(narrow)
				.count()).isEqualTo(2);

		Unifiable<String> everyone = lvar();
		assertThat(person.exists(source, lvar(), everyone)
				.solve(everyone)
				.count()).isEqualTo(3);
	}

	@Test
	public void answersMatchTheUnpushedSourceOnDomains() {
		List<String> pushed = domProgram(pushing());
		List<String> unpushed = domProgram(plain());
		assertThat(pushed).isEqualTo(unpushed);
		assertThat(pushed).hasSize(2);
	}

	@Test
	public void answersMatchTheUnpushedSourceOnComparisons() {
		List<String> pushed = leqProgram(pushing());
		List<String> unpushed = leqProgram(plain());
		assertThat(pushed).isEqualTo(unpushed);
		assertThat(pushed).hasSize(2);
	}

	@Test
	public void aNarrowerProbeAfterAPushedFetchStaysLocal() {
		SqlFactSource source = pushing();
		Unifiable<Long> wide = lvar();
		assertThat(dom(wide, EnumeratedDomain.range(1L, 4L))
				.and(person.exists(source, wide, lvar()))
				.solve(wide)
				.count()).isEqualTo(3);
		int afterPushed = statements.get();

		Unifiable<Long> narrower = lvar();
		assertThat(dom(narrower, EnumeratedDomain.range(1L, 3L))
				.and(person.exists(source, narrower, lvar()))
				.solve(narrower)
				.count()).isEqualTo(2);
		assertThat(statements.get())
				.describedAs("a probe whose region is contained in a covered one must stay local")
				.isEqualTo(afterPushed);
	}

	@Test
	public void theFdCompilerIsEquipment() {
		// no compiling(...) call anywhere: the FD compiler is wired at
		// pinned() — the domain still reaches the WHERE clause
		SqlFactSource source = SqlFactSource.pinned("h2-equipment", counting(connection));
		domProgram(source);
		assertThat(statementSql.stream().anyMatch(sql -> sql.contains("id IN (?, ?)")))
				.describedAs("the auto-registered FD compiler must push the domain")
				.isTrue();
	}

	@Test
	public void anAutoRegisteredCompilerIsOverridable() {
		// a user replacement takes the family over: an always-refusing
		// compiler keeps every FD atom local
		SqlFactSource source = SqlFactSource.pinned("h2-override", counting(connection))
				.compiling(FiniteDomainConstraints.class, (atom, columns) -> java.util.Optional.empty());
		domProgram(source);
		assertThat(statementSql.stream().noneMatch(sql -> sql.contains("IN (")))
				.describedAs("the override must replace the built-in")
				.isTrue();
	}

	@Test
	public void anUnregisteredFamilyStaysLocalAndAnswersRight() {
		// the nogood family has no compiler on this source: its exclusion is
		// not pushed, not consumed, and enforced locally — same answers as
		// the in-memory reference
		Unifiable<Long> viaSql = lvar();
		Unifiable<Long> viaMemory = lvar();
		List<String> sql = exclusionProgram(pushing(), viaSql);
		assertThat(sql).isEqualTo(exclusionProgram(reference, viaMemory));
		assertThat(sql).hasSize(2);
	}

	private static List<String> domProgram(FactSource source) {
		Unifiable<Long> x = lvar();
		Unifiable<String> out = lvar();
		return dom(x, EnumeratedDomain.range(1L, 3L))
				.and(person.exists(source, x, out))
				.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	private static List<String> leqProgram(FactSource source) {
		Unifiable<Long> x = lvar();
		Unifiable<String> out = lvar();
		return FiniteDomain.leq(x, lval(2L))
				.and(person.exists(source, x, out))
				.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	private static List<String> exclusionProgram(FactSource source, Unifiable<Long> x) {
		return exclude(x.unifies(2L))
				.and(person.exists(source, x, lvar()))
				.solve(x)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	/** Counts prepared statements, so the locality receipts can see fetches. */
	private Connection counting(Connection real) {
		return (Connection) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[]{Connection.class},
				(proxy, method, args) -> {
					if ("prepareStatement".equals(method.getName())) {
						statements.incrementAndGet();
						statementSql.add(String.valueOf(args[0]));
					}
					try {
						return method.invoke(real, args);
					} catch (java.lang.reflect.InvocationTargetException e) {
						throw e.getCause();
					}
				});
	}
}
