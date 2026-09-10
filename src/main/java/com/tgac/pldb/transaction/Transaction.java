package com.tgac.pldb.transaction;

// ABOUTME: The transaction: a read face, a write face staging facts, and a commit
// ABOUTME: proven by the source's serialization — one subtype per serialization kind.

import com.tgac.functional.category.Nothing;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One transaction over one serialized source: an {@link AnswerSource}
 * read face, a write face staging facts, and a {@link #commit()} proven
 * through the source's own serialization door. The serialization kind decides
 * the SUBTYPE at {@link #over}: an {@link Simulated} transaction records
 * every probe and certifies the whole log at commit (an
 * over-approximation — retries, never unsoundness); a {@link Native}
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

	/** The API face: literals in, the door converts — a hole refuses by column. */
	default Try<Transaction> withFacts(Literal... rows) {
		return withFacts(Arrays.stream(rows).map(Literal::fact).collect(Collectors.toList()));
	}

	/**
	 * The write face: the source's own commit door proves the binding and
	 * lands the flush. Success or refusal, the value is spent — close it.
	 */
	Try<Nothing> commit();
}
