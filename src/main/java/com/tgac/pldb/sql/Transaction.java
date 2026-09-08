package com.tgac.pldb.sql;

// ABOUTME: The transaction: a read face that records probes, a write face that
// ABOUTME: stages facts bound to them, and a commit proven by the source's certify.

import com.tgac.functional.category.Nothing;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.Certifiable;
import com.tgac.pldb.CertifiedReads;
import com.tgac.pldb.Footprint;
import com.tgac.pldb.Overlay;
import com.tgac.pldb.Pin;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import io.vavr.control.Try;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Value;

/**
 * One transaction over one certified source: the read face is an
 * {@link AnswerSource} that RECORDS every probe (all values of this
 * transaction share one log — any read here may justify any staged
 * fact, an over-approximation that costs retries, never soundness);
 * the write face stages facts bound to a snapshot of that log; and
 * {@link #commit()} is where the binding is proven — through the
 * source's own {@link Certifiable} protocol, or recognized from a
 * {@link CertifiedReads} backend. A source with neither capability
 * refuses at open: a transaction exists FOR its write face — reads
 * alone never need one.
 */
public final class Transaction implements AnswerSource, AutoCloseable {

	/** The world moved past this transaction's snapshot: re-solve and retry. */
	public static final class Conflict extends IllegalStateException {
		Conflict(String message) {
			super(message);
		}

		Conflict(String message, Throwable cause) {
			super(message, cause);
		}
	}

	@Value
	private static class Staged {
		List<Fact> facts;
		Footprint read;
	}

	private final Connection connection;
	private final AnswerSource source;
	private final Overlay overlay;
	private final Queue<Call<Relation>> reads;
	private final Vector<Staged> staged;
	private final Pin pinAtOpen;

	private Transaction(Connection connection, AnswerSource source, Overlay overlay,
			Queue<Call<Relation>> reads, Vector<Staged> staged, Pin pinAtOpen) {
		this.connection = connection;
		this.source = source;
		this.overlay = overlay;
		this.reads = reads;
		this.staged = staged;
		this.pinAtOpen = pinAtOpen;
	}

	public static Transaction over(Connection connection, AnswerSource source) {
		Pin pin = null;
		if (source instanceof Certifiable) {
			pin = ((Certifiable) source).pin();
		} else if (!(source instanceof CertifiedReads)) {
			throw new IllegalStateException(source.id()
					+ " exposes no certify capability — a transaction exists for its"
					+ " write face; read the source directly instead");
		}
		return new Transaction(connection, source, Overlay.over(source),
				new ConcurrentLinkedQueue<>(), Vector.empty(), pin);
	}

	public Try<Transaction> withFacts(List<Fact> facts) {
		Footprint read = Footprint.of(new ArrayList<>(reads));
		return overlay.withFacts(facts)
				.map(grown -> new Transaction(connection, source, grown, reads,
						staged.append(new Staged(facts, read)), pinAtOpen));
	}

	/**
	 * The write face: every staged binding certified, the flush landed,
	 * the transaction committed. Success or failure, the value is spent.
	 */
	public Try<Nothing> commit() {
		try {
			if (source instanceof Certifiable) {
				Certifiable certify = (Certifiable) source;
				boolean landed = certify.commit(pinAtOpen,
						staged.map(Staged::getRead),
						overlay.staged().asJava());
				rollBackQuietly();
				return landed
						? Try.success(Nothing.nothing())
						: Try.failure(new Conflict(id()
								+ ": a concurrent commit moved a region this transaction read — re-solve"));
			}
			SqlFlush.over(connection).flush(overlay.staged().asJava());
			connection.commit();
			return Try.success(Nothing.nothing());
		} catch (RuntimeException | SQLException e) {
			rollBackQuietly();
			SQLException recognized = recognizedConflict(e);
			if (recognized != null) {
				return Try.failure(new Conflict(id()
						+ ": a concurrent commit changed a region this transaction read — re-solve",
						recognized));
			}
			return Try.failure(e instanceof RuntimeException
					? (RuntimeException) e
					: new IllegalStateException(id() + ": commit failed", e));
		}
	}

	private SQLException recognizedConflict(Throwable e) {
		if (!(source instanceof CertifiedReads)) {
			return null;
		}
		CertifiedReads rented = (CertifiedReads) source;
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException && rented.conflict((SQLException) cause)) {
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
		reads.add(probe);
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
		rollBackQuietly();
		try {
			connection.close();
		} catch (SQLException suppressed) {
			// abandoning: nothing staged survives either way
		}
	}
}
