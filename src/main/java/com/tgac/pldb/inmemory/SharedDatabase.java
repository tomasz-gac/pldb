package com.tgac.pldb.inmemory;

// ABOUTME: The shared in-memory store: one mutable cell of persistent AnswerStore
// ABOUTME: values, opened as snapshots with simulated serialization per relation.

import com.tgac.logic.tabling.Call;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.transaction.Footprint;
import com.tgac.pldb.transaction.Pin;
import com.tgac.pldb.transaction.Pinned;
import com.tgac.pldb.transaction.SimulatedSerialization;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Value;

/**
 * The one history of an in-memory world: a mutable cell holding a
 * persistent {@link AnswerStore} value plus per-relation generations.
 * {@link #open} takes a snapshot — free, the value IS the snapshot —
 * and hands it out as a {@link SimulatedSerialization} source: reads
 * come from the captured value forever (stable by construction), the
 * pin is the captured generations, and commit is the memory tier's
 * whole protocol in one synchronized block — prove the footprint's
 * relations unmoved, grow the current value by the flush, bump the
 * moved generations. The degenerate ideal of the simulated ladder:
 * exact marks, a free snapshot, and the monitor as the commit lock.
 */
public final class SharedDatabase {

	@Value
	private static class Versioned {
		AnswerStore value;
		Map<String, Long> marks;
		long global;
	}

	private Versioned current;

	private SharedDatabase(AnswerStore initial) {
		this.current = new Versioned(initial, new HashMap<>(), 0);
	}

	public static SharedDatabase empty() {
		return new SharedDatabase(AnswerStore.empty());
	}

	public SimulatedSerialization open(String id) {
		return new Snapshot(id, read());
	}

	private synchronized Versioned read() {
		return current;
	}

	/**
	 * The commit protocol, whole: the monitor is the commit lock, the
	 * generation compare is the proof, the persistent grow is the flush.
	 */
	private synchronized boolean commit(Footprint read, java.util.List<Literal> flush) {
		if (!covers(read)) {
			return false;
		}
		AnswerStore grown = current.getValue();
		Map<String, Long> marks = new HashMap<>(current.getMarks());
		for (Literal fact : flush) {
			grown = grown.with(fact.getRel(), Answers.answer(fact.fact()));
			marks.merge(fact.getRel().getName(), 1L, Long::sum);
		}
		current = new Versioned(grown, marks, current.getGlobal() + 1);
		return true;
	}

	/** Exact per-relation marks: absence here is knowledge either way. */
	private boolean covers(Footprint read) {
		for (Map.Entry<Call<Relation>, Pin> pinned : read.pins().entrySet()) {
			MarksPin pin = (MarksPin) pinned.getValue();
			if (current.getGlobal() == pin.getGlobal()) {
				continue;
			}
			String relation = pinned.getKey().getRelation().getName();
			if (!Objects.equals(current.getMarks().get(relation), pin.getMarks().get(relation))) {
				return false;
			}
		}
		return true;
	}

	/** A snapshot of the cell: reads from the captured value, commits to the cell. */
	private final class Snapshot implements SimulatedSerialization {

		private final String id;
		private final Versioned captured;

		private Snapshot(String id, Versioned captured) {
			this.id = id;
			this.captured = captured;
		}

		@Override
		public Pinned<Iterable<Answer>> read(Call<Relation> probe) {
			return Pinned.of(captured.getValue().answers(probe),
					new MarksPin(captured.getMarks(), captured.getGlobal()));
		}

		@Override
		public boolean commit(Footprint read, java.util.List<Literal> flush) {
			return SharedDatabase.this.commit(read, flush);
		}

		@Override
		public Iterable<Answer> answers(Call<Relation> probe) {
			return captured.getValue().answers(probe);
		}

		@Override
		public long estimate(Call<Relation> probe) {
			return captured.getValue().estimate(probe);
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public void close() {
			// a snapshot holds no resources: abandonment is garbage collection
		}
	}

	/**
	 * The captured generations, WITHOUT the database value: pin equality
	 * is world identity (marks and global generation), never a content
	 * comparison — two pins of one world are equal, any commit between
	 * them makes them not.
	 */
	@Value
	private static class MarksPin implements Pin {
		Map<String, Long> marks;
		long global;
	}
}
