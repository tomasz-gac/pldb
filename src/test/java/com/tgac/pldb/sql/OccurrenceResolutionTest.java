package com.tgac.pldb.sql;

// ABOUTME: The discriminating receipt for Any resolution: a bound arg BEFORE a
// ABOUTME: constrained free makes occurrence and position diverge — pushed must
// ABOUTME: still agree with the unpushed oracle, or the WHERE hit the wrong column.

import static com.tgac.logic.finitedomain.FiniteDomain.dom;
import static com.tgac.logic.finitedomain.domains.EnumeratedDomain.range;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OccurrenceResolutionTest {

	private Connection connection;

	@Before
	public void loadH2() throws SQLException {
		connection = DriverManager.getConnection("jdbc:h2:mem:");
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE pair(a BIGINT NOT NULL, b BIGINT NOT NULL)");
			ddl.execute("INSERT INTO pair VALUES (1, 10), (1, 20), (2, 10)");
		}
	}

	@After
	public void closeH2() throws SQLException {
		connection.close();
	}

	private static Literal pair(AnswerSource db, Unifiable<Long> a, Unifiable<Long> b) {
		return Literal.relation(OccurrenceResolutionTest.class, "pair")
				.arg("a", a)
				.arg("b", b)
				.from(db);
	}

	/** Bound a=1, domain on the FREE b: the image is ({1}, _.0) — the atom's
	 * _.0 is COLUMN 1; positional resolution would compile the WHERE against
	 * column a and silently under-deliver. */
	private static List<Long> boundThenConstrained(AnswerSource db) {
		Unifiable<Long> b = lvar();
		return dom(b, range(10L, 16L))
				.and(pair(db, lval(1L), b))
				.solve(b)
				.map(Term::get)
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	public void aCoupledVariableResolvesToItsFirstOccurrence() throws SQLException {
		// pair(x, x) with dom(x): the constraint at ANY occurrence is implied
		// by the coupling, so the push narrows on column a and the coupling
		// itself filters locally — answers agree with the by-hand oracle
		try (Statement seed = connection.createStatement()) {
			seed.execute("INSERT INTO pair VALUES (2, 2), (3, 3), (5, 2)");
		}
		try (CachingSqlFetch pushed = CachingSqlFetch.pinned("h2-coupled", connection)) {
			Unifiable<Long> x = lvar();
			List<Long> agreed = dom(x, range(2L, 5L))
					.and(pair(pushed, x, x))
					.solve(x)
					.map(Term::get)
					.sorted()
					.collect(Collectors.toList());
			assertThat(agreed).containsExactly(2L, 3L);
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void aBoundArgBeforeAConstrainedFreeStillAgreesWithTheOracle() {
		try (CachingSqlFetch pushed = CachingSqlFetch.pinned("h2-occ", connection)) {
			assertThat(boundThenConstrained(pushed))
					.describedAs("the pushed WHERE must narrow column b, never column a")
					.containsExactly(10L);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
