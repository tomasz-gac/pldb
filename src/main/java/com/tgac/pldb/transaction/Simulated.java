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
 * SIMULATED serialization: this transaction's ledger is the read cache
 * AND the recorder in one — a region's FIRST touch reads through the
 * source (pin and rows minted from one world) and every repeat serves
 * the same {@link Pinned} back, so the transaction is its own snapshot
 * at region grain: read stability by construction, no source consulted
 * twice for one region. Commit folds the ledger's pins into the
 * {@link Footprint} by {@link Footprint#union} — regions are distinct
 * keys, so the union never conflicts here; its refusal guards the
 * cross-part compositions to come.
 */
public class Simulated extends AbstractTransaction {

	private final SimulatedSerialization serialization;
	private final ConcurrentMap<Call<Relation>, Pinned<Iterable<Answer>>> reads;

	Simulated(WriteBuffer writeBuffer, SimulatedSerialization serialization,
			ConcurrentMap<Call<Relation>, Pinned<Iterable<Answer>>> reads) {
		super(writeBuffer);
		this.serialization = serialization;
		this.reads = reads;
	}

	@Override
	public Iterable<Answer> answers(Call<Relation> probe) {
		Pinned<Iterable<Answer>> read = reads.computeIfAbsent(probe, serialization::read);
		return writeBuffer.overlay(probe, read.getValue());
	}

	@Override
	public Try<Transaction> withFacts(Collection<Literal> facts) {
		return writeBuffer.withFacts(new ArrayList<>(facts))
				.map(grown -> new Simulated(grown, serialization, reads));
	}

	@Override
	public Try<Nothing> commit() {
		Footprint footprint = reads.entrySet().stream()
				.map(read -> Footprint.of(read.getKey(), read.getValue().getPin()))
				.reduce(Footprint.empty(), Footprint::union);
		return through(() -> serialization.commit(footprint, writeBuffer.staged().asJava()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		serialization.close();
	}
}
