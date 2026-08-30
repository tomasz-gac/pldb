package com.tgac.pldb.sql;

// ABOUTME: The JDBC-backed AnswerSource: a constructor and a wrapper — the caching
// ABOUTME: source over the pinned SQL fetch, plus the registration and close doors.

import com.tgac.logic.finitedomain.FiniteDomainConstraints;
import com.tgac.logic.nogoods.NogoodConstraints;
import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Emitter;
import com.tgac.functional.fibers.Fiber;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.AnswerProducer;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.TabledSource;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import java.sql.Connection;

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
 * <p>This class is a constructor and a wrapper: {@link TabledSource}
 * over the pinned {@link SqlFetch}. The fetch owns the backend — the
 * connection, the per-family compiler registry, probe+region compiled to
 * SELECT..WHERE, every get a round trip. The cache owns streaming reuse:
 * {@link #produce} lands probes in the tabled compression. The SYNC face
 * fetches RAW: its probes come from the GAC tier, whose regions
 * transcribe the posted lookup itself — data to the WHERE compiler, but
 * a production loop if re-imposed as a goal, so they must never enter
 * the compression. What remains here is the lifecycle the composition
 * needs: pinning at construction, compiler registration before first
 * use, and {@link #close()} rolling the transaction back. Estimates are
 * exact over covered probes and the optimizer barrier otherwise — never
 * a remote round trip.
 *
 * <p>The connection is shared and the composed get synchronized: parallel
 * solves serialize their fetches here. Landed answers are immutable
 * snapshots, so reads outside the monitor stay safe.
 */
public final class SqlFactSource implements AnswerSource, AnswerProducer, AutoCloseable {

	private final SqlFetch fetch;
	private final TabledSource cached;

	private SqlFactSource(SqlFetch fetch) {
		this.fetch = fetch;
		this.cached = TabledSource.over((AnswerSource) fetch);
	}

	/**
	 * Pins the connection and wires the ENGINE-CORE compilers as equipment —
	 * the FD family pushes out of the box. {@link #compiling} registers user
	 * families and may OVERRIDE a built-in.
	 */
	public static SqlFactSource pinned(String id, Connection connection) {
		SqlFetch fetch = SqlFetch.pinned(id, connection);
		fetch.compiling(FiniteDomainConstraints.class, new FiniteDomainSqlCompiler());
		fetch.compiling(NogoodConstraints.class, new NogoodSqlCompiler(fetch.compilers()));
		return new SqlFactSource(fetch);
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
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		return fetch.answers(probe);
	}

	@Override
	public Fiber<Nothing> produce(Call<Relation> probe, Emitter<Tuple2<Reified<?>, Condition>> emit) {
		return cached.produce(probe, emit);
	}

	@Override
	public long estimate(Call<Relation> probe) {
		return cached.estimate(probe);
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
