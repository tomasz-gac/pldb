package com.tgac.pldb.sql;

// ABOUTME: The JDBC-backed FactSource: the relation IS the table (name and
// ABOUTME: property names verbatim), probes compile to SELECT..WHERE over the
// ABOUTME: pinned connection, fetches land, subsumed probes serve locally.

import com.tgac.pldb.Database;
import com.tgac.pldb.FactSource;
import com.tgac.pldb.ImmutableDatabase;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A relation backend over one pinned JDBC connection, by CONVENTION: the
 * database schema matches the pldb schema, so the relation's name is the
 * table and its property names are the columns — no mapping layer, and
 * values must be JDBC-representable. The source declares which relations
 * it serves; an unserved relation refuses loudly as a config error.
 *
 * <p>Pinning happens at construction: auto-commit off, {@code REPEATABLE
 * READ} where the driver's metadata admits it (recorded either way — a
 * backend that cannot promise the level still answers, at its declared
 * capability), and the snapshot anchored by a first read. {@link #close()}
 * rolls the transaction back — the source never writes.
 *
 * <p>A probe fetches only when no already-covered probe subsumes it (same
 * relation, bound positions a subset with equal values — the wider fetch
 * already landed every fact the narrower probe could match). Fetched rows
 * land in an internal in-memory database and every answer is served from
 * its index, so the propagation loop's narrowing probes after the first
 * wide fetch run without round trips. Estimates are exact over covered
 * probes and the optimizer barrier ({@code Long.MAX_VALUE}) otherwise —
 * never a remote round trip.
 *
 * <p>The connection is shared and synchronized: parallel solves serialize
 * their fetches here. Landed answers are immutable snapshots, so reads
 * outside the monitor stay safe.
 */
public final class SqlFactSource implements FactSource, AutoCloseable {

	private final String id;
	private final Connection connection;
	private final Set<Relation> served;
	private final int isolation;

	private Database landed = ImmutableDatabase.empty();
	private final Map<Relation, List<IndexedSeq<Optional<Object>>>> covered = new HashMap<>();

	private SqlFactSource(String id, Connection connection, Set<Relation> served, int isolation) {
		this.id = id;
		this.connection = connection;
		this.served = served;
		this.isolation = isolation;
	}

	public static SqlFactSource pinned(String id, Connection connection, Relation... served) {
		try {
			connection.setAutoCommit(false);
			if (connection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ)) {
				connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			}
			try (Statement anchor = connection.createStatement()) {
				anchor.execute("SELECT 1");
			}
			return new SqlFactSource(id, connection,
					new HashSet<>(Arrays.asList(served)), connection.getTransactionIsolation());
		} catch (SQLException e) {
			throw new IllegalStateException("could not pin " + id, e);
		}
	}

	@Override
	public String id() {
		return id;
	}

	/** The isolation level the backend actually granted — the pin's declared capability. */
	public int isolation() {
		return isolation;
	}

	@Override
	public synchronized Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args) {
		refuseUnserved(relation);
		if (!coveredBy(relation, args)) {
			land(relation, fetch(relation, args));
			covered.computeIfAbsent(relation, r -> new ArrayList<>()).add(args);
		}
		return landed.get(relation, args);
	}

	@Override
	public synchronized long estimate(Relation relation, IndexedSeq<Optional<Object>> args) {
		refuseUnserved(relation);
		return coveredBy(relation, args) ?
				landed.estimate(relation, args) :
				Long.MAX_VALUE;
	}

	@Override
	public void close() {
		try {
			connection.rollback();
			connection.close();
		} catch (SQLException e) {
			throw new IllegalStateException("could not close " + id, e);
		}
	}

	private void refuseUnserved(Relation relation) {
		if (!served.contains(relation)) {
			throw new IllegalArgumentException(id + " does not serve " + relation.getId());
		}
	}

	private boolean coveredBy(Relation relation, IndexedSeq<Optional<Object>> probe) {
		return covered.getOrDefault(relation, java.util.Collections.emptyList())
				.stream()
				.anyMatch(prior -> subsumes(prior, probe));
	}

	/** A wider probe subsumes a narrower one: its bound positions are a subset, values equal. */
	private static boolean subsumes(IndexedSeq<Optional<Object>> wide, IndexedSeq<Optional<Object>> narrow) {
		for (int i = 0; i < wide.size(); i++) {
			if (wide.get(i).isPresent()
					&& !wide.get(i).equals(narrow.get(i))) {
				return false;
			}
		}
		return true;
	}

	private void land(Relation relation, List<Fact> rows) {
		Set<Fact> resident = new HashSet<>();
		for (Fact fact : landed.get(relation, allFree(relation))) {
			resident.add(fact);
		}
		List<Fact> fresh = rows.stream()
				.filter(row -> !resident.contains(row))
				.collect(Collectors.toList());
		if (!fresh.isEmpty()) {
			landed = landed.withFacts(fresh)
					.getOrElseThrow(e -> new IllegalStateException("could not land rows of " + id, e));
		}
	}

	private static IndexedSeq<Optional<Object>> allFree(Relation relation) {
		return Array.fill(relation.getArgs().length, Optional.empty());
	}

	private List<Fact> fetch(Relation relation, IndexedSeq<Optional<Object>> args) {
		Property<?>[] columns = relation.getArgs();
		List<String> boundColumns = new ArrayList<>();
		List<Object> boundValues = new ArrayList<>();
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i).isPresent()) {
				boundColumns.add(columns[i].getName());
				boundValues.add(args.get(i).get());
			}
		}
		StringBuilder sql = new StringBuilder("SELECT ")
				.append(Arrays.stream(columns)
						.map(Property::getName)
						.collect(Collectors.joining(", ")))
				.append(" FROM ")
				.append(relation.getName());
		if (!boundColumns.isEmpty()) {
			sql.append(" WHERE ")
					.append(boundColumns.stream()
							.map(c -> c + " = ?")
							.collect(Collectors.joining(" AND ")));
		}
		try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
			for (int i = 0; i < boundValues.size(); i++) {
				statement.setObject(i + 1, boundValues.get(i));
			}
			try (ResultSet rows = statement.executeQuery()) {
				List<Fact> facts = new ArrayList<>();
				while (rows.next()) {
					Object[] values = new Object[columns.length];
					for (int i = 0; i < values.length; i++) {
						values[i] = rows.getObject(columns[i].getName());
					}
					facts.add(Fact.of(relation, Array.of(values)));
				}
				return facts;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("fetch failed on " + id + ": " + sql, e);
		}
	}

	@Override
	public String toString() {
		return id + served.stream()
				.map(Relation::getName)
				.collect(Collectors.joining(", ", "[", "]"));
	}
}
