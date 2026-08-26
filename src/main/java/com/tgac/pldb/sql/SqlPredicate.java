package com.tgac.pldb.sql;

// ABOUTME: One adapter-side predicate value: a WHERE fragment with its parameters
// ABOUTME: bound positionally — what a registered compiler produces from an atom.

import io.vavr.collection.Array;
import java.util.stream.StreamSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * The compiled form of one constraint atom, in the adapter's own vocabulary:
 * a SQL condition fragment plus the values it binds, joined into the fetch's
 * WHERE by conjunction. Values are always bound as parameters, never inlined
 * into the text. The vocabulary is deliberately flat — atomic conditions
 * under AND; the useful disjunction (a domain's value set) is {@link #in}.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlPredicate {
	String fragment;
	Array<Object> parameters;

	public static SqlPredicate in(String column, Iterable<?> values) {
		Array<Object> bound = Array.ofAll(StreamSupport.stream(values.spliterator(), false)
				.map(Object.class::cast));
		return new SqlPredicate(
				column + bound.map(v -> "?").mkString(" IN (", ", ", ")"),
				bound);
	}

	public static SqlPredicate between(String column, Object lo, Object hi) {
		return new SqlPredicate(column + " BETWEEN ? AND ?", Array.of(lo, hi));
	}

	public static SqlPredicate eq(String column, Object value) {
		return new SqlPredicate(column + " = ?", Array.of(value));
	}

	@Override
	public String toString() {
		return fragment + parameters.mkString(" [", ", ", "]");
	}
}
