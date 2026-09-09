package com.tgac.pldb.sql.transaction;

import com.tgac.functional.category.Nothing;
import com.tgac.pldb.CertifiedReads;
import com.tgac.pldb.WriteBuffer;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.sql.Transaction;
import io.vavr.control.Try;
import java.util.List;

/**
 * The RENTED tier: the backend tracks every read it serves, so this
 * transaction records nothing — it buffers writes and hands the flush
 * to the source's {@link CertifiedReads} door.
 */
public class NativeSerialization extends AbstractTransaction {

	private final CertifiedReads certified;

	NativeSerialization(WriteBuffer writeBuffer, CertifiedReads certified) {
		super(writeBuffer);
		this.certified = certified;
	}

	@Override
	public Try<Transaction> withFacts(List<Fact> facts) {
		return writeBuffer.withFacts(facts)
				.map(grown -> new NativeSerialization(grown, certified));
	}

	@Override
	public Try<Nothing> commit() {
		return through(() -> certified.commit(writeBuffer.staged().asJava()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		certified.close();
	}
}