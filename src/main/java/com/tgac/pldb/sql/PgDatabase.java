package com.tgac.pldb.sql;

// ABOUTME: The transactional store value: a SERIALIZABLE snapshot under an Overlay,
// ABOUTME: commit flushes the staged delta and maps a serialization failure to Conflict.

import com.tgac.functional.category.Nothing;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.Overlay;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * A store value over one PostgreSQL transaction: the base is a
 * SERIALIZABLE snapshot read through the pinned fetch, appends grow a
 * private {@link Overlay} delta, and {@link #commit()} is the write
 * face — flush the staged facts, commit the transaction. Certify is
 * RENTED from SSI: the database tracks every region this transaction
 * read, and a concurrent commit that changed one surfaces here as a
 * serialization failure, mapped to {@link Conflict} — the caller's move
 * is an ordinary re-solve against a fresh value. Values sharing one
 * connection share one transaction: forked siblings are legal readers,
 * but the first commit spends the transaction for all of them.
 * {@link #close()} abandons the transaction; nothing staged survives.
 */
public final class PgDatabase implements AnswerSource, AutoCloseable {

	/** The world moved past this value's snapshot: re-solve and retry. */
	public static final class Conflict extends IllegalStateException {
		Conflict(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private final Connection connection;
	private final SqlFactSource snapshot;
	private final Overlay overlay;

	private PgDatabase(Connection connection, SqlFactSource snapshot, Overlay overlay) {
		this.connection = connection;
		this.snapshot = snapshot;
		this.overlay = overlay;
	}

	public static PgDatabase open(String id, Connection connection) {
		try {
			connection.setAutoCommit(false);
			connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		} catch (SQLException e) {
			throw new IllegalStateException("could not open " + id + " serializable", e);
		}
		SqlFactSource snapshot = SqlFactSource.pinned(id, connection);
		return new PgDatabase(connection, snapshot, Overlay.over(snapshot));
	}

	public Try<PgDatabase> withFacts(List<Fact> facts) {
		return overlay.withFacts(facts)
				.map(grown -> new PgDatabase(connection, snapshot, grown));
	}

	/**
	 * The write face: staged facts land as INSERTs and the transaction
	 * commits. A serialization failure — SSI proving that a concurrent
	 * commit changed a region this transaction read — rolls back and
	 * answers {@link Conflict}. Success or failure, the value is spent.
	 */
	public Try<Nothing> commit() {
		try {
			SqlFlush.over(connection).flush(overlay.staged().asJava());
			connection.commit();
			return Try.success(Nothing.nothing());
		} catch (RuntimeException | SQLException e) {
			rollBackQuietly();
			SQLException sql = serializationFailure(e);
			if (sql != null) {
				return Try.failure(new Conflict(
						id() + ": a concurrent commit changed a region this value read — re-solve", sql));
			}
			return Try.failure(e instanceof RuntimeException
					? (RuntimeException) e
					: new IllegalStateException(id() + ": commit failed", e));
		}
	}

	/** PostgreSQL reports SSI aborts as SQLSTATE 40001, anywhere in the cause chain. */
	private static SQLException serializationFailure(Throwable e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException
					&& "40001".equals(((SQLException) cause).getSQLState())) {
				return (SQLException) cause;
			}
		}
		return null;
	}

	private void rollBackQuietly() {
		try {
			connection.rollback();
		} catch (SQLException suppressed) {
			// the transaction is already dead; the caller gets the original failure
		}
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		return overlay.answers(probe);
	}

	@Override
	public long estimate(Call<Relation> probe) {
		return overlay.estimate(probe);
	}

	@Override
	public String id() {
		return overlay.id();
	}

	@Override
	public void close() {
		snapshot.close();
	}
}
