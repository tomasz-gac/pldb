package com.tgac.pldb.sql;

// ABOUTME: Pins the adapter-side predicate values: fragment text and parameter
// ABOUTME: binding, receipted by running each rendered WHERE against H2.

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SqlPredicateTest {

	private Connection connection;

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

	@Test
	public void inRendersAMembershipTest() throws SQLException {
		SqlPredicate p = SqlPredicate.in("id", Arrays.asList(1, 3));
		assertThat(p.getFragment()).isEqualTo("id IN (?, ?)");
		assertThat(selectNames(p)).containsExactly("Ada", "Kurt");
	}

	@Test
	public void betweenRendersAnInclusiveRange() throws SQLException {
		SqlPredicate p = SqlPredicate.between("id", 2, 3);
		assertThat(p.getFragment()).isEqualTo("id BETWEEN ? AND ?");
		assertThat(selectNames(p)).containsExactly("Alan", "Kurt");
	}

	@Test
	public void eqRendersAnEquality() throws SQLException {
		SqlPredicate p = SqlPredicate.eq("name", "Ada");
		assertThat(p.getFragment()).isEqualTo("name = ?");
		assertThat(selectNames(p)).containsExactly("Ada");
	}

	@Test
	public void comparisonsRenderAgainstAValue() throws SQLException {
		assertThat(SqlPredicate.leq("id", 2).getFragment()).isEqualTo("id <= ?");
		assertThat(selectNames(SqlPredicate.leq("id", 2))).containsExactly("Ada", "Alan");
		assertThat(SqlPredicate.lss("id", 2).getFragment()).isEqualTo("id < ?");
		assertThat(selectNames(SqlPredicate.lss("id", 2))).containsExactly("Ada");
		assertThat(SqlPredicate.geq("id", 2).getFragment()).isEqualTo("id >= ?");
		assertThat(selectNames(SqlPredicate.geq("id", 2))).containsExactly("Alan", "Kurt");
		assertThat(SqlPredicate.gtr("id", 2).getFragment()).isEqualTo("id > ?");
		assertThat(selectNames(SqlPredicate.gtr("id", 2))).containsExactly("Kurt");
	}

	@Test
	public void disequalityRendersBothWays() throws SQLException {
		assertThat(SqlPredicate.neq("id", 2).getFragment()).isEqualTo("id <> ?");
		assertThat(selectNames(SqlPredicate.neq("id", 2))).containsExactly("Ada", "Kurt");

		SqlPredicate columns = SqlPredicate.neqColumns("id", "boss");
		assertThat(columns.getFragment()).isEqualTo("id <> boss");
		assertThat(columns.getParameters().isEmpty()).isTrue();
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("ALTER TABLE person ADD COLUMN boss INT");
			ddl.execute("UPDATE person SET boss = 2");
		}
		assertThat(selectNames(columns)).containsExactly("Ada", "Kurt");
	}

	@Test
	public void columnComparisonRendersParameterless() throws SQLException {
		SqlPredicate p = SqlPredicate.leqColumns("id", "boss");
		assertThat(p.getFragment()).isEqualTo("id <= boss");
		assertThat(p.getParameters().isEmpty()).isTrue();
		assertThat(SqlPredicate.lssColumns("id", "boss").getFragment()).isEqualTo("id < boss");

		try (Statement ddl = connection.createStatement()) {
			ddl.execute("ALTER TABLE person ADD COLUMN boss INT");
			ddl.execute("UPDATE person SET boss = 2");
		}
		assertThat(selectNames(p)).containsExactly("Ada", "Alan");
	}

	@Test
	public void columnEqualityRendersParameterless() throws SQLException {
		SqlPredicate p = SqlPredicate.eqColumns("id", "boss");
		assertThat(p.getFragment()).isEqualTo("id = boss");
		assertThat(p.getParameters().isEmpty()).isTrue();
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("ALTER TABLE person ADD COLUMN boss INT");
			ddl.execute("UPDATE person SET boss = 2");
		}
		assertThat(selectNames(p)).containsExactly("Alan");
	}

	private List<String> selectNames(SqlPredicate predicate) throws SQLException {
		String sql = "SELECT name FROM person WHERE " + predicate.getFragment() + " ORDER BY id";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			int index = 1;
			for (Object parameter : predicate.getParameters()) {
				statement.setObject(index++, parameter);
			}
			try (ResultSet rows = statement.executeQuery()) {
				List<String> names = new ArrayList<>();
				while (rows.next()) {
					names.add(rows.getString("name"));
				}
				return names;
			}
		}
	}
}
