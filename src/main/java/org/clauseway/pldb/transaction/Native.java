package org.clauseway.pldb.transaction;

import org.clauseway.pldb.relations.Answer;
import java.util.List;

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
	public Transaction asserting(List<Answer> rows) {
		return new Native(writeBuffer.asserting(rows), serialization);
	}

	@Override
	public Transaction retracting(List<Answer> rows) {
		return new Native(writeBuffer.retracting(rows), serialization);
	}

	@Override
	public void commit() throws Conflict {
		through(() -> serialization.commit(
				writeBuffer.stagedAssertions(),
				writeBuffer.stagedRetractions()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		serialization.close();
	}
}