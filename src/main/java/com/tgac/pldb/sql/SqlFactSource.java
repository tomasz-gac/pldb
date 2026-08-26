package com.tgac.pldb.sql;

// ABOUTME: The JDBC-backed FactSource: a constructor and a wrapper — the caching
// ABOUTME: source over the pinned SQL fetch, plus the registration and close doors.

import com.tgac.logic.tabling.Residues;
import com.tgac.pldb.FactSource;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.IndexedSeq;
import java.sql.Connection;
import java.util.Optional;

/**
 * A relation backend over one pinned JDBC connection, by CONVENTION: the
 * database schema matches the pldb schema, so the relation's name is the
 * table and its property names are the columns — no mapping layer, and
 * values must be JDBC-representable; columns backing relation properties
 * are NON-NULL (SQL's three-valued logic would silently drop NULL rows
 * from pushed comparisons — under-delivery — while the engine has no null
 * vocabulary at all). The backend is the schema authority: a relation
 * without a table fails loudly at its first fetch.
 *
 * <p>This class is a constructor and a wrapper: {@link CachingFactSource}
 * over the pinned {@link SqlFetch}. The fetch owns the backend — the
 * connection, the per-family compiler registry, probe+region compiled to
 * SELECT..WHERE, every get a round trip. The cache owns reuse — landing,
 * the coverage ledger, containment proof. What remains here is the
 * lifecycle the composition needs: pinning at construction, compiler
 * registration before first use, and {@link #close()} rolling the
 * transaction back. Estimates are exact over covered probes and the
 * optimizer barrier otherwise — never a remote round trip.
 *
 * <p>The connection is shared and the composed get synchronized: parallel
 * solves serialize their fetches here. Landed answers are immutable
 * snapshots, so reads outside the monitor stay safe.
 */
public final class SqlFactSource implements FactSource, AutoCloseable {

	private final SqlFetch fetch;
	private final CachingFactSource cached;

	private SqlFactSource(SqlFetch fetch) {
		this.fetch = fetch;
		this.cached = CachingFactSource.over(fetch);
	}

	public static SqlFactSource pinned(String id, Connection connection) {
		return new SqlFactSource(SqlFetch.pinned(id, connection));
	}

	/**
	 * Registers the family's WHERE compiler. Before first use only — a
	 * registry changing under live coverage would make containment
	 * order-dependent.
	 */
	public SqlFactSource compiling(Class<?> family, SqlCompiler compiler) {
		if (!cached.isEmpty()) {
			throw new IllegalStateException(id() + ": register compilers before first use");
		}
		fetch.compiling(family, compiler);
		return this;
	}

	@Override
	public String id() {
		return fetch.id();
	}

	/** The isolation level the backend actually granted — the pin's declared capability. */
	public int isolation() {
		return fetch.isolation();
	}

	@Override
	public Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args) {
		return cached.get(relation, args);
	}

	@Override
	public Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args, Residues region) {
		return cached.get(relation, args, region);
	}

	@Override
	public long estimate(Relation relation, IndexedSeq<Optional<Object>> args) {
		return cached.estimate(relation, args);
	}

	@Override
	public void close() {
		fetch.close();
	}

	@Override
	public String toString() {
		return cached.toString();
	}
}
