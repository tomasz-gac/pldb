package com.tgac.pldb.sql;

// ABOUTME: The rented certify tier: a source under honest SERIALIZABLE isolation,
// ABOUTME: declaring its backend's conflict dialect through CertifiedReads.

import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.CertifiedReads;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Predicate;

/**
 * A pinned source whose backend certifies reads itself: the connection
 * is granted SERIALIZABLE before pinning, and the factory that mints
 * this wrapper VOUCHES that the backend's SERIALIZABLE actually
 * validates read sets (PostgreSQL's SSI, two-phase-locking
 * implementations) and speaks its conflict dialect. A backend whose
 * SERIALIZABLE is snapshot isolation in costume must not come through
 * here — no dialect predicate can make it honest.
 */
public final class SerializableSource implements AnswerSource, CertifiedReads, AutoCloseable {

	private final SqlFactSource inner;
	private final Predicate<SQLException> dialect;

	private SerializableSource(SqlFactSource inner, Predicate<SQLException> dialect) {
		this.inner = inner;
		this.dialect = dialect;
	}

	/** PostgreSQL: SSI, refusals as the standard serialization-failure SQLSTATE. */
	public static SerializableSource postgres(String id, Connection connection) {
		return pinned(id, connection, e -> "40001".equals(e.getSQLState()));
	}

	public static SerializableSource pinned(String id, Connection connection,
			Predicate<SQLException> dialect) {
		try {
			connection.setAutoCommit(false);
			connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		} catch (SQLException e) {
			throw new IllegalStateException("could not open " + id + " serializable", e);
		}
		return new SerializableSource(SqlFactSource.pinned(id, connection), dialect);
	}

	@Override
	public boolean conflict(SQLException failure) {
		return dialect.test(failure);
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		return inner.answers(probe);
	}

	@Override
	public long estimate(Call<Relation> probe) {
		return inner.estimate(probe);
	}

	@Override
	public String id() {
		return inner.id();
	}

	@Override
	public void close() {
		inner.close();
	}
}
