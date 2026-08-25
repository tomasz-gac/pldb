package com.tgac.pldb.sql;

// ABOUTME: One column's binding: its SQL name plus the hand-written codec that
// ABOUTME: turns a fact value into a JDBC parameter and a result cell back.

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * The per-column half of a {@link SqlMapping}: aligned positionally with the
 * relation's properties. The identity codec ({@code setObject}/{@code
 * getObject}) covers JDBC-native types; anything richer — enums by name,
 * temporal conversions — is written per column, never reflected.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlColumn {
	String name;
	Binder binder;
	Reader reader;

	public interface Binder {
		void bind(PreparedStatement statement, int index, Object value) throws SQLException;
	}

	public interface Reader {
		Object read(ResultSet row, String column) throws SQLException;
	}

	public static SqlColumn of(String name) {
		return new SqlColumn(name, PreparedStatement::setObject, ResultSet::getObject);
	}

	public static SqlColumn of(String name, Binder binder, Reader reader) {
		return new SqlColumn(name, binder, reader);
	}

	@Override
	public String toString() {
		return name;
	}
}
