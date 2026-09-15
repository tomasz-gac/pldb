package com.tgac.pldb.transaction;

import com.tgac.functional.category.Nothing;
import com.tgac.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;

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
	public Try<Transaction> asserting(Collection<Literal> facts) {
		return writeBuffer.asserting(new ArrayList<>(facts))
				.map(grown -> new Native(grown, serialization));
	}

	@Override
	public Try<Transaction> retracting(Collection<Literal> facts) {
		return writeBuffer.retracting(facts)
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