package com.tgac.pldb;

// ABOUTME: The one container: an owned table of sealed answer cells over any
// ABOUTME: producer — claim-once production, channel streaming, call subsumption.

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Emitter;
import com.tgac.functional.fibers.Fiber;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.JoinMap;
import com.tgac.logic.tabling.Table;
import com.tgac.logic.tabling.TableEntry;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.ArrayList;
import java.util.List;

/**
 * Memoization as a decorator over any {@link AnswerSource}: probes land in
 * an OWNED table of answer cells (per-source lifetime, persistent between
 * solves — sealed entries are portable values), keyed by their whole call
 * and served by subsumption: wide covers narrow, a narrower region never
 * serves a wider probe. {@link #produce} is the streaming face — the
 * ASYNC kind: the first claimant's workforce pumps the wrapped producer
 * into the cell (a sync producer blocks inside the claimed workforce —
 * the inline cost it always had), every claimant replays the cell's log,
 * parking on growth, completed by the seal; concurrent probes cannot
 * double-fetch, losers of the claim read as consumers. The cell is
 * term → {@link Condition}, so conditional answers land natively and
 * duplicates fold inertly — dedup is the join's own algebra.
 *
 * <p>The SYNC {@link AnswerSource} face serves SEALED entries only:
 * unknown is not false, so an uncovered probe refuses loudly rather than
 * answering empty.
 */
public final class TabledSource implements AnswerSource {

	private final AnswerSource producer;
	private final Table table = Table.empty();

	private TabledSource(AnswerSource producer) {
		this.producer = producer;
	}

	/** Produce-on-miss over the wrapped producer. */
	public static TabledSource over(AnswerSource producer) {
		return new TabledSource(producer);
	}

	/**
	 * The streaming face: claim the probe's entry (or join the claimant),
	 * replay the cell's log — emissions arrive as the cell grows, and the
	 * seal ends the stream.
	 */
	public Fiber<Nothing> produce(Call<Relation> probe, Emitter<Tuple2<Reified<?>, Condition>> emit) {
		TableEntry<Object> entry = entryFor(probe);
		return Fiber.produce(entry.channel(), inner -> pump(probe, entry, inner))
				.flatMap(claimed -> replay(entry, emit, 0));
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		TableEntry<Object> sealed = table.findSealedSubsumer(probe);
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

	/** The claimed workforce: the producer's answers, folded into the cell. */
	private Fiber<Nothing> pump(Call<Relation> probe, TableEntry<Object> entry,
			Emitter<JoinMap<Reified<?>, Object>> inner) {
		Fiber<Nothing> emissions = Fiber.done(Nothing.nothing());
		for (Tuple2<Reified<?>, Condition> answer : producer.answers(probe)) {
			JoinMap<Reified<?>, Object> delta = entry.answerDelta(answer._1, answer._2);
			emissions = emissions.flatMap(emitted -> inner.emit(delta));
		}
		return emissions;
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
