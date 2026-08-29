package com.tgac.pldb;

// ABOUTME: The one container: an owned table of sealed answer cells over any
// ABOUTME: AnswerProducer — the derived relation memoized for inter-solve reuse.

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Emitter;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.fibers.schedulers.BreadthFirstScheduler;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.JoinMap;
import com.tgac.logic.tabling.Table;
import com.tgac.logic.tabling.TableEntry;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The table as the source: an OWNED table of answer cells over any
 * {@link AnswerProducer}, keyed by the whole call and served by
 * subsumption — wide covers narrow, a narrower region never serves a
 * wider probe. The table outlives any solve (sealed entries are portable
 * values; validity over time is the pins' job), which is the point:
 * {@link #solving} memoizes a DERIVED RELATION for inter-solve reuse and,
 * eventually, persistence — the entries are what a memo store marshals.
 *
 * <p>{@link #produce} implements the async kind: the first claimant
 * detaches the wrapped producer's OWN produce as the entry's workforce
 * (registration returns immediately — the workforce runs billed to the
 * cell's scope, and the seal arrives from that ledger when it finishes);
 * every claimant, winner included, replays the cell's log by cursor,
 * parking on growth, completed by the seal. Concurrent probes cannot
 * double-fetch: losers of the claim CAS read as consumers. The cell is
 * term → {@link Condition}: conditional answers land natively, duplicates
 * fold inertly — dedup is the join's own algebra.
 *
 * <p>The SYNC {@link AnswerSource} face serves SEALED entries — and, for a
 * LIFTED sync producer, populates inline first: that produce cannot park
 * (a sync pump completes before the replay of a sealed cell), so driving
 * it to completion on the caller's thread is deterministic, the same
 * inline cost the sync kind always paid. For any other producer an
 * uncovered probe REFUSES: unknown is not false.
 */
public final class TabledSource implements AnswerSource, AnswerProducer {

	private final AnswerProducer producer;
	private final Table table;

	private TabledSource(AnswerProducer producer, Table table) {
		this.producer = producer;
		this.table = table;
	}

	/** Produce-on-miss over the wrapped producer. */
	public static TabledSource over(AnswerProducer producer) {
		return new TabledSource(producer, Table.empty());
	}

	/** The sync kind, lifted at the composition point. */
	public static TabledSource over(AnswerSource source) {
		return over(AnswerProducer.of(source));
	}

	/**
	 * The derived relation: a goal over positional arguments, memoized into
	 * the owned table. The body runs from the key, caller-agnostic, and
	 * SHARES the table — inner tabled calls accumulate beside the derived
	 * entries.
	 */
	public static TabledSource solving(Function<Array<Unifiable<?>>, Goal> body) {
		Table shared = Table.empty();
		return new TabledSource(new GoalSource(body, shared), shared);
	}

	@Override
	public Fiber<Nothing> produce(Call<Relation> probe, Emitter<Tuple2<Reified<?>, Condition>> emit) {
		TableEntry<Object> entry = entryFor(probe);
		return Fiber.produce(entry.channel(), inner -> producer.produce(probe, folding(entry, inner)))
				.flatMap(registered -> replay(entry, emit, 0));
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		TableEntry<Object> sealed = table.findSealedSubsumer(probe);
		if (sealed == null && producer instanceof SyncLift) {
			new BreadthFirstScheduler<>(produce(probe, answer -> Fiber.done(Nothing.nothing()))).get();
			sealed = table.findSealedSubsumer(probe);
		}
		if (sealed == null) {
			throw new IllegalStateException("no sealed entry covers " + probe
					+ ": unknown is not false — probe through produce, or seal first");
		}
		JoinMap<Reified<?>, Object> cell = sealed.answers();
		List<Tuple2<Reified<?>, Condition>> answers = new ArrayList<>();
		for (Reified<?> term : cell.order) {
			answers.add(Tuple.of(term, (Condition) cell.members.get(term).get()));
		}
		return answers;
	}

	@Override
	public long estimate(Call<Relation> probe) {
		TableEntry<Object> sealed = table.findSealedSubsumer(probe);
		return sealed != null ? sealed.getAnswerCount() : producer.estimate(probe);
	}

	@Override
	public String id() {
		return producer.id();
	}

	/** The exact entry, a subsuming one (open included — joining is sound), or fresh. */
	private TableEntry<Object> entryFor(Call<Relation> probe) {
		TableEntry<Object> subsumer = table.reusableSubsumer(probe);
		return subsumer != null ? subsumer : table.getOrCreateEntry(probe);
	}

	/** The producer's emissions folded into the cell as deltas. */
	private static Emitter<Tuple2<Reified<?>, Condition>> folding(TableEntry<Object> entry,
			Emitter<JoinMap<Reified<?>, Object>> inner) {
		return answer -> inner.emit(entry.answerDelta(answer._1, answer._2));
	}

	/** The cell's log by cursor: emit, park on growth, the seal completes. */
	private Fiber<Nothing> replay(TableEntry<Object> entry,
			Emitter<Tuple2<Reified<?>, Condition>> emit, int cursor) {
		JoinMap<Reified<?>, Object> now = entry.answers();
		if (cursor < now.logSize()) {
			Tuple2<Reified<?>, Object> logged = now.logAt(cursor);
			return emit.emit(Tuple.of(logged._1, (Condition) logged._2))
					.flatMap(emitted -> replay(entry, emit, cursor + 1));
		}
		if (entry.isComplete()) {
			return Fiber.done(Nothing.nothing());
		}
		int at = cursor;
		return Fiber.await(entry.channel(), value -> value.logSize() > at)
				.flatMap(grown -> replay(entry, emit, at));
	}

	@Override
	public String toString() {
		return "tabled(" + producer + ")";
	}
}
