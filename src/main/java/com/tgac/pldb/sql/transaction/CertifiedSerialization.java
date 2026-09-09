package com.tgac.pldb.sql.transaction;

import com.tgac.functional.category.Nothing;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.Certifiable;
import com.tgac.pldb.Footprint;
import com.tgac.pldb.Pin;
import com.tgac.pldb.WriteBuffer;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.sql.Transaction;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * The OWNED tier: this transaction is the read-tracker — every probe
 * lands in the log, and commit hands the pin, the log's footprint and
 * the flush to the source's {@link Certifiable} door.
 */
public class CertifiedSerialization extends AbstractTransaction {

	private final Certifiable certify;
	private final Queue<Call<Relation>> reads;
	private final Pin pinAtOpen;

	CertifiedSerialization(WriteBuffer writeBuffer, Certifiable certify,
			Queue<Call<Relation>> reads, Pin pinAtOpen) {
		super(writeBuffer);
		this.certify = certify;
		this.reads = reads;
		this.pinAtOpen = pinAtOpen;
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		reads.add(probe);
		return super.answers(probe);
	}

	@Override
	public Try<Transaction> withFacts(List<Fact> facts) {
		return writeBuffer.withFacts(facts)
				.map(grown -> new CertifiedSerialization(grown, certify, reads, pinAtOpen));
	}

	@Override
	public Try<Nothing> commit() {
		return through(() -> certify.commit(pinAtOpen,
				Footprint.of(new ArrayList<>(reads)),
				writeBuffer.staged().asJava()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		certify.close();
	}
}