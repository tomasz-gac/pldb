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
