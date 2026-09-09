package com.tgac.pldb.sql.transaction;

// ABOUTME: The transaction: a read face, a write face staging facts, and a commit
// ABOUTME: proven by the source's certify — one subtype per certify capability.

import com.tgac.functional.category.Nothing;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.Certifiable;
import com.tgac.pldb.CertifiedReads;
import com.tgac.pldb.WriteBuffer;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.sql.Transaction;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * One transaction over one certified source: an {@link AnswerSource}
 * read face, a write face staging facts, and a {@link #commit()} proven
 * through the source's own certify door. The certify capability decides
 * the SUBTYPE at {@link #over}: an {@link CertifiedSerialization} transaction records
 * every probe and certifies the whole log at commit (an
 * over-approximation — retries, never unsoundness); a {@link NativeSerialization}
 * transaction records nothing, because its backend tracks the reads
 * itself. A source with neither capability has no {@code over} to call:
 * a transaction exists FOR its write face — reads alone never need one.
 */
@RequiredArgsConstructor(access = AccessLevel.MODULE)
public abstract class AbstractTransaction implements Transaction {
	final WriteBuffer writeBuffer;

	public static AbstractTransaction over(Certifiable source) {
		return new CertifiedSerialization(WriteBuffer.over(source), source,
				new ConcurrentLinkedQueue<>(), source.pin());
	}

	public static AbstractTransaction over(CertifiedReads source) {
		return new NativeSerialization(WriteBuffer.over(source), source);
	}

	/** The shared verdict mapping: refused = Conflict, anything thrown surfaces. */
	Try<Nothing> through(BooleanSupplier door) {
		try {
			return door.getAsBoolean()
					? Try.success(Nothing.nothing())
					: Try.failure(new Transaction.Conflict(id()
					+ ": a concurrent commit moved a region this transaction read — re-solve"));
		} catch (RuntimeException e) {
			return Try.failure(e);
		}
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		return writeBuffer.answers(probe);
	}

	@Override
	public long estimate(Call<Relation> probe) {
		return writeBuffer.estimate(probe);
	}

	@Override
	public String id() {
		return writeBuffer.id();
	}
}
