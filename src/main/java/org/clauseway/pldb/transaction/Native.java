package org.clauseway.pldb.transaction;

import org.clauseway.functional.category.Nothing;
import org.clauseway.pldb.relations.Answer;
import java.util.List;
import io.vavr.control.Try;

/**
 * NATIVE serialization: the backend tracks every read it serves, so this
 * transaction records nothing — it buffers writes and hands the flush
 * to the source's {@link NativeSerialization} door.
 */
public class Native extends AbstractTransaction {

	private final NativeSerialization serialization;

	Native(WriteBuffer writeBuffer, NativeSerialization serialization) {
		super(writeBuffer);
		this.serialization = serialization;
	}

	@Override
	public Try<Transaction> asserting(List<Answer> rows) {
		return writeBuffer.asserting(rows)
				.map(grown -> new Native(grown, serialization));
	}

	@Override
	public Try<Transaction> retracting(List<Answer> rows) {
		return writeBuffer.retracting(rows)
				.map(marked -> new Native(marked, serialization));
	}

	@Override
	public Try<Nothing> commit() {
		return through(() -> serialization.commit(
				writeBuffer.stagedAssertions().asJava(),
				writeBuffer.stagedRetractions().asJava()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		serialization.close();
	}
}