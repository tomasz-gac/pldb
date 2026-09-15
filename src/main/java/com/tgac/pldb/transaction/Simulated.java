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
 * twice for one region. Commit proves the {@link #footprint()} of its
 * own reads UNIONED with the {@link #requiring(Footprint) premise} —
 * regions a client read in an EARLIER request, whose decision this
 * write carries out: the commit stands on both, and either world
 * having moved refuses it. The union's own refusal guards overlapping
 * regions read at different worlds.
 */
public class Simulated extends AbstractTransaction {

	private final SimulatedSerialization serialization;
	private final ConcurrentMap<Call<Relation>, Pinned<Iterable<Answer>>> reads;
	private final Footprint premise;

	Simulated(WriteBuffer writeBuffer, SimulatedSerialization serialization,
			ConcurrentMap<Call<Relation>, Pinned<Iterable<Answer>>> reads, Footprint premise) {
		super(writeBuffer);
		this.serialization = serialization;
		this.reads = reads;
		this.premise = premise;
	}

	@Override
	public Iterable<Answer> answers(Call<Relation> probe) {
		Pinned<Iterable<Answer>> read = reads.computeIfAbsent(probe, serialization::read);
		return writeBuffer.overlay(probe, read.getValue());
	}

	@Override
	public Try<Transaction> asserting(Collection<Literal> facts) {
		return writeBuffer.asserting(new ArrayList<>(facts))
				.map(grown -> new Simulated(grown, serialization, reads, premise));
	}

	/** The ledger folded: every region this transaction read, at its pin. */
	public Footprint footprint() {
		return reads.entrySet().stream()
				.map(read -> Footprint.of(read.getKey(), read.getValue().getPin()))
				.reduce(Footprint.empty(), Footprint::union);
	}

	/**
	 * The client's premise: regions read ELSEWHERE (an earlier request,
	 * another transaction) that this commit must also prove unmoved —
	 * the decision behind the write stood on them, whether or not this
	 * transaction reads them itself. Premises accumulate by union.
	 */
	public Simulated requiring(Footprint required) {
		return new Simulated(writeBuffer, serialization, reads, premise.union(required));
	}

	@Override
	public Try<Nothing> commit() {
		return through(() -> serialization.commit(
				footprint().union(premise), writeBuffer.staged().asJava()));
	}

	/** Ends the snapshot (the source's close rolls its read transaction back). */
	@Override
	public void close() throws Exception {
		serialization.close();
	}
}
