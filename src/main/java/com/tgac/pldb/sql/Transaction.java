package com.tgac.pldb.sql;

// ABOUTME: The transaction: a read face, a write face staging facts, and a commit
// ABOUTME: proven by the source's certify — one subtype per certify capability.

import com.tgac.functional.category.Nothing;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.sql.transaction.CertifiedSerialization;
import com.tgac.pldb.sql.transaction.NativeSerialization;
import io.vavr.control.Try;
import java.util.List;

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
public interface Transaction extends AnswerSource, AutoCloseable {

	final class Conflict extends Exception {
		public Conflict(String message) {
			super(message);
		}
	}

	Try<Transaction> withFacts(List<Fact> facts);

	/**
	 * The write face: the source's own commit door proves the binding and
	 * lands the flush. Success or refusal, the value is spent — close it.
	 */
	Try<Nothing> commit();
}
