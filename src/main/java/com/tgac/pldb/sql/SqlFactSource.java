package com.tgac.pldb.sql;

// ABOUTME: The JDBC-backed FactSource: the relation IS the table (name and
// ABOUTME: property names verbatim), probes compile to SELECT..WHERE over the
// ABOUTME: pinned connection, fetches land, subsumed probes serve locally.

import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.unification.Any;
import com.tgac.pldb.Database;
import com.tgac.pldb.FactSource;
import com.tgac.pldb.ImmutableDatabase;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * A relation backend over one pinned JDBC connection, by CONVENTION: the
 * database schema matches the pldb schema, so the relation's name is the
 * table and its property names are the columns — no mapping layer, and
 * values must be JDBC-representable. The backend is the schema authority:
 * a relation without a table fails loudly at its first fetch.
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
	private final int isolation;

	private Database landed = ImmutableDatabase.empty();
	private final Map<Relation, List<Coverage>> covered = new HashMap<>();
	private final Map<Class<?>, SqlCompiler> compilers = new HashMap<>();

	/** One completed fetch: the probe's pattern plus the region its WHERE enforced. */
	@Value
	private static class Coverage {
		IndexedSeq<Optional<Object>> pattern;
		Residues consumed;
	}

	/** A compilation's outcome: the predicates pushed, the atoms they consumed. */
	@Value
	private static class Pushed {
		List<SqlPredicate> predicates;
		Residues consumed;
	}

	private SqlFactSource(String id, Connection connection, int isolation) {
		this.id = id;
		this.connection = connection;
		this.isolation = isolation;
	}

	public static SqlFactSource pinned(String id, Connection connection) {
		try {
			connection.setAutoCommit(false);
			if (connection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ)) {
				connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			}
			try (Statement anchor = connection.createStatement()) {
				anchor.execute("SELECT 1");
			}
			return new SqlFactSource(id, connection, connection.getTransactionIsolation());
		} catch (SQLException e) {
			throw new IllegalStateException("could not pin " + id, e);
		}
	}

	/**
	 * Registers the family's WHERE compiler. Before first use only — a
	 * registry changing under live coverage would make containment
	 * order-dependent.
	 */
	public SqlFactSource compiling(Class<?> family, SqlCompiler compiler) {
		if (!covered.isEmpty()) {
			throw new IllegalStateException(id + ": register compilers before first use");
		}
		compilers.put(family, compiler);
		return this;
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
	public Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args) {
		return get(relation, args, Residues.TRUE);
	}

	@Override
	public synchronized Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args, Residues region) {
		if (!coveredBy(relation, args, region)) {
			Pushed pushed = push(relation, region);
			land(relation, fetch(relation, args, pushed.getPredicates()));
			covered.computeIfAbsent(relation, r -> new ArrayList<>())
					.add(new Coverage(args, pushed.getConsumed()));
		}
		return landed.get(relation, args);
	}

	/** Every registered family's atoms through its compiler: predicates + consumed. */
	private Pushed push(Relation relation, Residues region) {
		List<SqlPredicate> predicates = new ArrayList<>();
		io.vavr.collection.Map<Class<?>, Theory<?>> consumed = io.vavr.collection.HashMap.empty();
		for (Tuple2<Class<?>, Theory<?>> family : region.getTheories()) {
			SqlCompiler compiler = compilers.get(family._1);
			if (compiler == null) {
				continue;
			}
			List<Atom<?>> taken = new ArrayList<>();
			for (Atom<?> atom : family._2.atoms()) {
				Optional<SqlPredicate> compiled = compiler.compile(atom, columnResolver(relation));
				if (compiled.isPresent()) {
					predicates.add(compiled.get());
					taken.add(atom);
				}
			}
			if (!taken.isEmpty()) {
				consumed = consumed.put(family._1, theoryOf(taken));
			}
		}
		return new Pushed(predicates, Residues.of(consumed));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Theory<?> theoryOf(List<Atom<?>> atoms) {
		return Theory.of((Iterable) atoms);
	}

	/** Positional names resolve to columns: {@code _.i} is the i-th property. */
	private static SqlCompiler.ColumnResolver columnResolver(Relation relation) {
		return term -> {
			if (!(term instanceof Any)) {
				return Optional.empty();
			}
			int position = ((Any<?>) term).getNumber();
			return position >= 0 && position < relation.getArgs().length ?
					Optional.of(relation.getArgs()[position].getName()) :
					Optional.empty();
		};
	}

	@Override
	public synchronized long estimate(Relation relation, IndexedSeq<Optional<Object>> args) {
		return coveredBy(relation, args, Residues.TRUE) ?
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

	/**
	 * Serve locally only on PROOF: the probe's pattern subsumed by a covered
	 * one AND the probe's full region entailing the atoms that fetch's WHERE
	 * enforced ({@code Residues.leq} true = containment proven). A pushed
	 * fetch landed only its region's rows — pattern subsumption alone would
	 * claim completeness for rows it never fetched, and absence reads as
	 * falsity. leq answering false merely re-fetches, idempotently.
	 */
	private boolean coveredBy(Relation relation, IndexedSeq<Optional<Object>> probe, Residues region) {
		return covered.getOrDefault(relation, java.util.Collections.emptyList())
				.stream()
				.anyMatch(prior -> subsumes(prior.getPattern(), probe)
						&& region.leq(prior.getConsumed()));
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

	private List<Fact> fetch(Relation relation, IndexedSeq<Optional<Object>> args, List<SqlPredicate> predicates) {
		Property<?>[] columns = relation.getArgs();
		List<String> boundColumns = new ArrayList<>();
		List<Object> boundValues = new ArrayList<>();
		List<String> unboundColumns = new ArrayList<>();
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i).isPresent()) {
				boundColumns.add(columns[i].getName());
				boundValues.add(args.get(i).get());
			} else {
				unboundColumns.add(columns[i].getName());
			}
		}
		StringBuilder sql = buildSqlStatement(relation, unboundColumns, boundColumns, predicates);
		try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
			int index = 1;
			for (Object bound : boundValues) {
				statement.setObject(index++, bound);
			}
			for (SqlPredicate predicate : predicates) {
				for (Object parameter : predicate.getParameters()) {
					statement.setObject(index++, parameter);
				}
			}
			try (ResultSet rows = statement.executeQuery()) {
				List<Fact> facts = new ArrayList<>();
				while (rows.next()) {
					Object[] values = new Object[unboundColumns.size()];
					for (int i = 0; i < values.length; i++) {
						values[i] = rows.getObject(unboundColumns.get(i));
					}
					Array<Object> vals = mergeValuesWithSupplied(args, values);
					facts.add(Fact.of(relation, Array.ofAll(vals)));
				}
				return facts;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("fetch failed on " + id + ": " + sql, e);
		}
	}

	private static Array<Object> mergeValuesWithSupplied(IndexedSeq<Optional<Object>> args, Object[] values) {
		int i = 0, j = 0;
		Array<Object> result = Array.empty();
		while (i + j < args.length()) {
			if (args.get(i + j).isPresent()) {
				result = result.append(args.get(i + j).get());
				++i;
			} else {
				result = result.append(values[j]);
				++j;
			}
		}
		return result;
	}

	private static StringBuilder buildSqlStatement(Relation relation, List<String> unboundColumns,
			List<String> boundColumns, List<SqlPredicate> predicates) {
		// every position bound: nothing to project, the probe is an existence
		// check — a blank select list would not compile
		StringBuilder sql = new StringBuilder("SELECT ")
				.append(unboundColumns.isEmpty() ? "1" : String.join(", ", unboundColumns))
				.append(" FROM ")
				.append(relation.getName());
		List<String> conditions = new ArrayList<>();
		for (String column : boundColumns) {
			conditions.add(column + " = ?");
		}
		for (SqlPredicate predicate : predicates) {
			conditions.add(predicate.getFragment());
		}
		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" AND ", conditions));
		}
		return sql;
	}

	@Override
	public String toString() {
		return id;
	}
}
