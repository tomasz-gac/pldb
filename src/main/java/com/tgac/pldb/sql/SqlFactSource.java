package com.tgac.pldb.sql;

// ABOUTME: The JDBC-backed FactSource, composed: SqlFetch polls the pinned backend,
// ABOUTME: Landing reuses covered fetches subsumptively — this shell just wires them.

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
 * <p>Two halves compose here and this shell only wires them. {@link
 * SqlFetch} owns the backend: the pinned connection, the per-family
 * compiler registry, probe+region to rows plus the CONSUMED region its
 * WHERE enforced. {@link Landing} owns reuse: the landed pool and the
 * coverage ledger, serving a probe locally only on containment proof.
 * A probe that no covered fetch proves fetches once and lands; the
 * propagation loop's narrowing probes after a wide fetch run without
 * round trips. Estimates are exact over covered probes and the optimizer
 * barrier ({@code Long.MAX_VALUE}) otherwise — never a remote round trip.
 *
 * <p>The connection is shared and synchronized: parallel solves serialize
 * their fetches here. Landed answers are immutable snapshots, so reads
 * outside the monitor stay safe.
 */
public final class SqlFactSource implements FactSource, AutoCloseable {

	private final String id;
	private final SqlFetch fetch;
	private final Landing landing = new Landing();

	private SqlFactSource(String id, SqlFetch fetch) {
		this.id = id;
		this.fetch = fetch;
	}

	public static SqlFactSource pinned(String id, Connection connection) {
		return new SqlFactSource(id, SqlFetch.pinned(id, connection));
	}

	/**
	 * Registers the family's WHERE compiler. Before first use only — a
	 * registry changing under live coverage would make containment
	 * order-dependent.
	 */
	public SqlFactSource compiling(Class<?> family, SqlCompiler compiler) {
		if (!landing.isEmpty()) {
			throw new IllegalStateException(id + ": register compilers before first use");
		}
		fetch.compiling(family, compiler);
		return this;
	}

	@Override
	public String id() {
		return id;
	}

	/** The isolation level the backend actually granted — the pin's declared capability. */
	public int isolation() {
		return fetch.isolation();
	}

	@Override
	public Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args) {
		return get(relation, args, Residues.TRUE);
	}

	@Override
	public synchronized Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args, Residues region) {
		if (!landing.covers(relation, args, region)) {
			SqlFetch.Fetched fetched = fetch.fetch(relation, args, region);
			landing.land(relation, args, fetched.getConsumed(), fetched.getRows());
		}
		return landing.serve(relation, args);
	}

	@Override
	public synchronized long estimate(Relation relation, IndexedSeq<Optional<Object>> args) {
		return landing.covers(relation, args, Residues.TRUE) ?
				landing.estimate(relation, args) :
				Long.MAX_VALUE;
	}

	@Override
	public void close() {
		fetch.close();
	}

	@Override
	public String toString() {
		return id;
	}
}
