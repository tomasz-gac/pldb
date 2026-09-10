package com.tgac.pldb.transaction;

import com.tgac.functional.category.Nothing;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

/**
 * SIMULATED serialization: this transaction is the read-tracker — every probe
 * lands in the log, and commit hands the pin, the log's footprint and
 * the flush to the source's {@link SimulatedSerialization} door.
 */
public class Simulated extends AbstractTransaction {

	private final SimulatedSerialization serialization;
	private final Queue<Call<Relation>> reads;
	private final Pin pinAtOpen;

	Simulated(WriteBuffer writeBuffer, SimulatedSerialization serialization,
			Queue<Call<Relation>> reads, Pin pinAtOpen) {
		super(writeBuffer);
		this.serialization = serialization;
		this.reads = reads;
		this.pinAtOpen = pinAtOpen;
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		reads.add(probe);
		return super.answers(probe);
	}

	@Override
	public Try<Transaction> withFacts(Collection<Literal> facts) {
		return writeBuffer.withFacts(new ArrayList<>(facts))
				.map(grown -> new Simulated(grown, serialization, reads, pinAtOpen));
	}

	@Override
	public Try<Nothing> commit() {
		return through(() -> serialization.commit(pinAtOpen,
				Footprint.of(new ArrayList<>(reads)),
				writeBuffer.staged().asJava()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		serialization.close();
	}
}