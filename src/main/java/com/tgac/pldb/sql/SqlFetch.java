package com.tgac.pldb.sql;

// ABOUTME: The SQL polling half of the source: one pinned connection, the compiler
// ABOUTME: registry, and probe+region -> rows plus the region the WHERE enforced.

import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.unification.Any;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Value;

/**
 * Talks to the backend, nothing else: pins the connection at construction
 * (auto-commit off, {@code REPEATABLE READ} where the driver's metadata
 * admits it, the granted level recorded, the snapshot anchored by a first
 * read), compiles a probe's region through the registered per-family
 * compilers, executes the SELECT, and reports the rows TOGETHER WITH the
 * consumed region — exactly the atoms the WHERE enforced, which is what a
 * caching layer must record to reuse the fetch soundly. Holds no pool and
 * no ledger; every call is a round trip.
 */
final class SqlFetch {

	private final String id;
	private final Connection connection;
	private final int isolation;
	private final Map<Class<?>, SqlCompiler> compilers = new HashMap<>();

	/** One poll's outcome: the rows, and the region the WHERE enforced. */
	@Value
	static class Fetched {
		List<Fact> rows;
		Residues consumed;
	}

	private SqlFetch(String id, Connection connection, int isolation) {
		this.id = id;
		this.connection = connection;
		this.isolation = isolation;
	}

	static SqlFetch pinned(String id, Connection connection) {
		try {
			connection.setAutoCommit(false);
			if (connection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ)) {
				connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			}
			try (Statement anchor = connection.createStatement()) {
				anchor.execute("SELECT 1");
			}
			return new SqlFetch(id, connection, connection.getTransactionIsolation());
		} catch (SQLException e) {
			throw new IllegalStateException("could not pin " + id, e);
		}
	}

	int isolation() {
		return isolation;
	}

	void compiling(Class<?> family, SqlCompiler compiler) {
		compilers.put(family, compiler);
	}

	Fetched fetch(Relation relation, IndexedSeq<Optional<Object>> args, Residues region) {
		Pushed pushed = push(relation, region);
		return new Fetched(rows(relation, args, pushed.getPredicates()), pushed.getConsumed());
	}

	void close() {
		try {
			connection.rollback();
			connection.close();
		} catch (SQLException e) {
			throw new IllegalStateException("could not close " + id, e);
		}
	}

	/** A compilation's outcome: the predicates pushed, the atoms they consumed. */
	@Value
	private static class Pushed {
		List<SqlPredicate> predicates;
		Residues consumed;
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

	private List<Fact> rows(Relation relation, IndexedSeq<Optional<Object>> args, List<SqlPredicate> predicates) {
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
}
