package com.tgac.pldb.sql;

// ABOUTME: One relation's binding to a table: the table name plus one SqlColumn
// ABOUTME: per property position — the declared, never-reflected schema bridge.

import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlMapping {
	Relation relation;
	String table;
	Array<SqlColumn> columns;

	public static SqlMapping of(Relation relation, String table, SqlColumn... columns) {
		if (columns.length != relation.getArgs().length) {
			throw new IllegalArgumentException(String.format(
					"%s has %d properties, the mapping to %s names %d columns",
					relation.getId(), relation.getArgs().length, table, columns.length));
		}
		return new SqlMapping(relation, table, Array.of(columns));
	}

	@Override
	public String toString() {
		return relation.getName() + " -> " + table + columns.mkString("(", ", ", ")");
	}
}
