package com.tgac.pldb.sql;

// ABOUTME: The transactional store value: a SERIALIZABLE snapshot under an Overlay,
// ABOUTME: commit flushes the staged delta; a conflict predicate names the refusal.

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
import java.util.function.Predicate;

/**
 * A store value over one SERIALIZABLE transaction: the base is the
 * snapshot read through the pinned fetch, appends grow a private
 * {@link Overlay} delta, and {@link #commit()} is the write face —
 * flush the staged facts, commit the transaction. Certify is RENTED
 * from the backend's serializable isolation: it tracks every region
 * this transaction read, and a concurrent commit that changed one
 * surfaces at commit as an exception the CONFLICT PREDICATE recognizes,
 * mapped to {@link Conflict} — the caller's move is an ordinary
 * re-solve against a fresh value. The default predicate matches the
 * standard serialization-failure SQLSTATE (40001: PostgreSQL's SSI,
 * MySQL and SQL Server's two-phase locking). The renting PRECONDITION
 * is semantic and no predicate can supply it: the backend's
 * SERIALIZABLE must actually validate read sets — a backend whose
 * SERIALIZABLE is snapshot isolation in costume (Oracle) admits write
 * skew no matter what is matched. Values sharing one connection share
 * one transaction: forked siblings are legal readers, but the first
 * commit spends the transaction for all of them. {@link #close()}
 * abandons the transaction; nothing staged survives.
 */
public final class SerializedDatabase implements AnswerSource, AutoCloseable {

	/** The world moved past this value's snapshot: re-solve and retry. */
	public static final class Conflict extends IllegalStateException {
		Conflict(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private final Connection connection;
	private final SqlFactSource snapshot;
	private final Overlay overlay;
	private final Predicate<SQLException> conflict;

	private SerializedDatabase(Connection connection, SqlFactSource snapshot, Overlay overlay,
			Predicate<SQLException> conflict) {
		this.connection = connection;
		this.snapshot = snapshot;
		this.overlay = overlay;
		this.conflict = conflict;
	}

	/** Opens with the standard serialization-failure SQLSTATE as the conflict predicate. */
	public static SerializedDatabase open(String id, Connection connection) {
		return open(id, connection, e -> "40001".equals(e.getSQLState()));
	}

	public static SerializedDatabase open(String id, Connection connection, Predicate<SQLException> conflict) {
		try {
			connection.setAutoCommit(false);
			connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		} catch (SQLException e) {
			throw new IllegalStateException("could not open " + id + " serializable", e);
		}
		SqlFactSource snapshot = SqlFactSource.pinned(id, connection);
		return new SerializedDatabase(connection, snapshot, Overlay.over(snapshot), conflict);
	}

	public Try<SerializedDatabase> withFacts(List<Fact> facts) {
		return overlay.withFacts(facts)
				.map(grown -> new SerializedDatabase(connection, snapshot, grown, conflict));
	}

	/**
	 * The write face: staged facts land as INSERTs and the transaction
	 * commits. A failure the conflict predicate recognizes — the backend
	 * proving that a concurrent commit changed a region this transaction
	 * read — rolls back and answers {@link Conflict}. Success or
	 * failure, the value is spent.
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

	/** The first cause the conflict predicate recognizes, anywhere in the chain. */
	private SQLException serializationFailure(Throwable e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException && conflict.test((SQLException) cause)) {
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
