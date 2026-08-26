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
 * Nested boolean structure and arithmetic operands are one deferred design
 * decision (the AST); its reopening triggers are Union domains,
 * multi-literal nogoods, and an addo atom crossing a region.
 *
 * <p>CONVENTION: columns backing relation properties are NON-NULL. SQL's
 * three-valued logic makes every comparison silently drop NULL rows —
 * under-delivery, the one sin — while the engine has no null vocabulary
 * at all; the schema-matching convention therefore excludes them.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlPredicate {
	String fragment;
	Array<Object> parameters;

	public static <T> SqlPredicate in(String column, Iterable<T> values) {
		Array<Object> bound = Array.ofAll(StreamSupport.stream(values.spliterator(), false)
				.map(Object.class::cast));
		return new SqlPredicate(
				column + bound.map(v -> "?").mkString(" IN (", ", ", ")"),
				bound);
	}

	public static <T> SqlPredicate between(String column, T lo, T hi) {
		return new SqlPredicate(column + " BETWEEN ? AND ?", Array.of(lo, hi));
	}

	public static <T> SqlPredicate eq(String column, T value) {
		return new SqlPredicate(column + " = ?", Array.of(value));
	}

	public static <T> SqlPredicate leq(String column, T value) {
		return new SqlPredicate(column + " <= ?", Array.of(value));
	}

	public static <T> SqlPredicate lss(String column, T value) {
		return new SqlPredicate(column + " < ?", Array.of(value));
	}

	public static <T> SqlPredicate geq(String column, T value) {
		return new SqlPredicate(column + " >= ?", Array.of(value));
	}

	public static <T> SqlPredicate gtr(String column, T value) {
		return new SqlPredicate(column + " > ?", Array.of(value));
	}

	public static <T> SqlPredicate neq(String column, T value) {
		return new SqlPredicate(column + " <> ?", Array.of(value));
	}

	/** Column against column — parameterless; a distinct name, since a String value would be ambiguous. */
	public static SqlPredicate eqColumns(String left, String right) {
		return new SqlPredicate(left + " = " + right, Array.empty());
	}

	/** Column against column — parameterless; a distinct name, since a String value would be ambiguous. */
	public static SqlPredicate leqColumns(String less, String more) {
		return new SqlPredicate(less + " <= " + more, Array.empty());
	}

	/** Column against column — parameterless; a distinct name, since a String value would be ambiguous. */
	public static SqlPredicate lssColumns(String less, String more) {
		return new SqlPredicate(less + " < " + more, Array.empty());
	}

	/** Column against column — parameterless; a distinct name, since a String value would be ambiguous. */
	public static SqlPredicate neqColumns(String left, String right) {
		return new SqlPredicate(left + " <> " + right, Array.empty());
	}

	@Override
	public String toString() {
		return fragment + parameters.mkString(" [", ", ", "]");
	}
}
