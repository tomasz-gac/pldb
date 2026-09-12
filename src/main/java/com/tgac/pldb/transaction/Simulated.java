package com.tgac.pldb.transaction;

import com.tgac.functional.category.Nothing;
import com.tgac.logic.tabling.Call;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * SIMULATED serialization: this transaction is the read-tracker — every
 * region pins at its FIRST touch, BEFORE the read it certifies (the
 * ordering {@link SimulatedSerialization} stands on), and commit hands
 * the pinned footprint and the flush to the source's door.
 */
public class Simulated extends AbstractTransaction {

	private final SimulatedSerialization serialization;
	private final ConcurrentMap<Call<Relation>, Pin> reads;

	Simulated(WriteBuffer writeBuffer, SimulatedSerialization serialization,
			ConcurrentMap<Call<Relation>, Pin> reads) {
		super(writeBuffer);
		this.serialization = serialization;
		this.reads = reads;
	}

	@Override
	public Iterable<Answer> answers(Call<Relation> probe) {
		reads.computeIfAbsent(probe, serialization::pin);
		return super.answers(probe);
	}

	@Override
	public Try<Transaction> withFacts(Collection<Literal> facts) {
		return writeBuffer.withFacts(new ArrayList<>(facts))
				.map(grown -> new Simulated(grown, serialization, reads));
	}

	@Override
	public Try<Nothing> commit() {
		return through(() -> serialization.commit(Footprint.of(reads),
				writeBuffer.staged().asJava()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		serialization.close();
	}
}
