package com.tgac.pldb.sql;

// ABOUTME: The JDBC write face: facts land as INSERTs by the schema convention —
// ABOUTME: relation name is the table, columns encode through the codec registry.

import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Flushes facts through one JDBC connection by the same convention the
 * fetch reads with: the relation's name is the table, its property names
 * are the columns. Every cell encodes through the CODEC registry — a
 * value without a codec refuses by relation and column, and a
 * structural value (a collection, a term) keeps the modelling refusal:
 * its relational spelling is a child relation. The whole batch encodes
 * before any row lands. The caller owns the transaction: this face
 * neither commits nor rolls back.
 */

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlFlush {
	Connection connection;
	Codecs codecs;

	public static SqlFlush over(Connection connection) {
		return new SqlFlush(connection, Codecs.builtin());
	}

	public static SqlFlush over(Connection connection, Codecs codecs) {
		return new SqlFlush(connection, codecs);
	}

	public void flush(List<Literal> literals) {
		List<Fact> facts = literals.stream().map(Literal::fact)
				.map(this::encoded)
				.collect(Collectors.toList());
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

	/**
	 * Every cell through the registry BEFORE any insert — the whole batch
	 * validates or none of it lands. A null cell rides the nullable
	 * declaration; everything else must have a codec.
	 */
	private Fact encoded(Fact fact) {
		Property<?>[] columns = fact.getRelation().getArgs();
		Object[] cells = new Object[columns.length];
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
			cells[i] = codecs.encode(fact.getRelation(), columns[i], value);
		}
		return Fact.of(fact.getRelation(), Array.of(cells));
	}
}
