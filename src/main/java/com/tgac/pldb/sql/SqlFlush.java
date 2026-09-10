package com.tgac.pldb.sql;

// ABOUTME: The JDBC write face: facts land as INSERTs by the schema convention —
// ABOUTME: relation name is the table, property names are the columns, atoms only.

import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flushes facts through one JDBC connection by the same convention the
 * fetch reads with: the relation's name is the table, its property names
 * are the columns. Column values must be ATOMS — a structural value (a
 * collection, a term) is a first-normal-form violation whose relational
 * spelling is a child relation, so it refuses by relation and column
 * name, and the whole batch is validated before any row lands. The
 * caller owns the transaction: this face neither commits nor rolls back.
 */
public final class SqlFlush {

	private final Connection connection;

	private SqlFlush(Connection connection) {
		this.connection = connection;
	}

	public static SqlFlush over(Connection connection) {
		return new SqlFlush(connection);
	}

	public void flush(List<Literal> literals) {
		List<Fact> facts = literals.stream().map(Literal::fact).collect(Collectors.toList());
		facts.forEach(SqlFlush::requireAtomColumns);
		Map<Relation, List<Fact>> byRelation = facts.stream()
				.collect(Collectors.groupingBy(Fact::getRelation, LinkedHashMap::new, Collectors.toList()));
		try {
			for (Map.Entry<Relation, List<Fact>> table : byRelation.entrySet()) {
				insert(table.getKey(), table.getValue());
			}
		} catch (SQLException e) {
			throw new IllegalStateException("flush failed: " + e.getMessage(), e);
		}
	}

	private void insert(Relation relation, List<Fact> rows) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(insertSql(relation))) {
			for (Fact row : rows) {
				for (int i = 0; i < row.getValues().size(); i++) {
					statement.setObject(i + 1, row.getValues().get(i));
				}
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private static String insertSql(Relation relation) {
		String columns = Arrays.stream(relation.getArgs())
				.map(Property::getName)
				.collect(Collectors.joining(", "));
		String holes = Arrays.stream(relation.getArgs())
				.map(p -> "?")
				.collect(Collectors.joining(", "));
		return "INSERT INTO " + relation.getName() + " (" + columns + ") VALUES (" + holes + ")";
	}

	private static void requireAtomColumns(Fact fact) {
		Property<?>[] columns = fact.getRelation().getArgs();
		for (int i = 0; i < columns.length; i++) {
			Object value = fact.getValues().get(i);
			if (value == null) {
				if (!columns[i].isNullable()) {
					throw new IllegalStateException("flush of " + fact.getRelation().getName()
							+ ": column '" + columns[i].getName() + "' is not nullable —"
							+ " null cannot land in it");
				}
				continue;
			}
			if (!isAtom(value)) {
				throw new IllegalStateException("flush of " + fact.getRelation().getName()
						+ ": column '" + columns[i].getName() + "' holds a structural value ("
						+ value.getClass().getSimpleName()
						+ ") — a first-normal-form violation; model it as a child relation");
			}
		}
	}

	private static boolean isAtom(Object value) {
		return value instanceof Number
				|| value instanceof String
				|| value instanceof Boolean
				|| value instanceof Character;
	}
}
