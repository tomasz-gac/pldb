package com.tgac.pldb.sql;

// ABOUTME: The SQL polling source: one pinned connection, the compiler registry,
// ABOUTME: the probe's pattern+region compiled to SELECT..WHERE — every answer a round trip.

import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.sql.compiler.SqlPredicate;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Talks to the backend, nothing else: pins the connection at construction
 * (auto-commit off, {@code REPEATABLE READ} where the driver's metadata
 * admits it, the granted level recorded, the snapshot anchored by a first
 * read), compiles a probe's region through the registered per-family
 * compilers into the WHERE, and executes the SELECT. Holds no pool and no
 * ledger; every get is a round trip, and the estimate is the optimizer
 * barrier. Package-private: callers compose through {@link CachingSqlFetch}
 * — the caching is not optional equipment.
 */
@Slf4j
final class SqlFetch implements JdbcSource {

	private final String id;
	@Getter
	private final Connection connection;
	private final int isolation;
	private final Map<Class<?>, SqlCompiler> compilers = new HashMap<>();

	private SqlFetch(String id, Connection connection, int isolation) {
		this.id = id;
		this.connection = connection;
		this.isolation = isolation;
	}

	static SqlFetch pinned(String id, Connection connection) {
		try {
			connection.setAutoCommit(false);
			// the pin promises AT LEAST a repeatable snapshot: raise a weaker
			// level, keep a stronger one (SERIALIZABLE is the rented certify)
			if (connection.getTransactionIsolation() < Connection.TRANSACTION_REPEATABLE_READ
					&& connection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ)) {
				connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			}
			// an MVCC snapshot begins at the transaction's FIRST READ, not at
			// setup — without this anchor, "pinned at construction" would
			// silently mean "pinned at the first probe", and writes landing
			// in between would leak into the view
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

	@Override
	public String id() {
		return id;
	}

	void compiling(Class<?> family, SqlCompiler compiler) {
		compilers.put(family, compiler);
	}

	/** The LIVE registry view — cross-family compilers (nogoods) delegate through it. */
	Map<Class<?>, SqlCompiler> compilers() {
		return compilers;
	}

	@Override
	public synchronized Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		Relation relation = probe.getRelation();
		IndexedSeq<Term<Object>> args = Answers.positions(probe.getArguments());
		List<Tuple2<Reified<?>, Condition>> answers = new ArrayList<>();
		for (Fact fact : rows(relation, args, push(relation, probe.getResidues()))) {
			answers.add(Answers.answer(fact));
		}
		return answers;
	}

	@Override
	public long estimate(Call<Relation> probe) {
		return Long.MAX_VALUE;
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

	/** Every registered family's atoms through its compiler; misses stay local. */
	private List<SqlPredicate> push(Relation relation, Residues region) {
		List<SqlPredicate> predicates = new ArrayList<>();
		SqlCompiler.ColumnResolver resolver = columnResolver(relation);
		for (Tuple2<Class<?>, Theory<?>> family : region.getTheories()) {
			SqlCompiler compiler = compilers.get(family._1);
			if (compiler == null) {
				continue;
			}
			for (Atom<?> atom : family._2.atoms()) {
				compiler.compile(atom, resolver)
						.ifPresent(predicates::add);
			}
		}
		return predicates;
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

	private List<Fact> rows(Relation relation, IndexedSeq<Term<Object>> args, List<SqlPredicate> predicates) {
		Property<?>[] columns = relation.getArgs();
		List<String> boundColumns = new ArrayList<>();
		List<Object> boundValues = new ArrayList<>();
		List<String> unboundColumns = new ArrayList<>();
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i).asVal().isDefined()) {
				boundColumns.add(columns[i].getName());
				boundValues.add(args.get(i).get());
			} else {
				unboundColumns.add(columns[i].getName());
			}
		}
		StringBuilder sql = buildSqlStatement(relation, unboundColumns, boundColumns, predicates);
		if (log.isDebugEnabled()) {
			List<Object> parameters = new ArrayList<>(boundValues);
			predicates.forEach(predicate -> predicate.getParameters().forEach(parameters::add));
			log.debug("{}: {} ← {}", id, sql, parameters);
		}
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

	private static Array<Object> mergeValuesWithSupplied(IndexedSeq<Term<Object>> args, Object[] values) {
		int i = 0, j = 0;
		Array<Object> result = Array.empty();
		while (i + j < args.length()) {
			if (args.get(i + j).asVal().isDefined()) {
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
