package com.tgac.pldb.transaction;

import com.tgac.functional.category.Nothing;
import com.tgac.pldb.relations.Fact;
import io.vavr.control.Try;
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
	public Try<Transaction> withFacts(List<Fact> facts) {
		return writeBuffer.withFacts(facts)
				.map(grown -> new Native(grown, serialization));
	}

	@Override
	public Try<Nothing> commit() {
		return through(() -> serialization.commit(writeBuffer.staged().asJava()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		serialization.close();
	}
}