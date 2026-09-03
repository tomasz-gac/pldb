package com.tgac.pldb;

// ABOUTME: The one container over any producer: probes memoize through the owned
// ABOUTME: table, wide sealed entries serve narrow probes, the only face streams.

import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.fibers.schedulers.BreadthFirstScheduler;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.Tabled;
import com.tgac.logic.tabling.Tabling;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.relations.RelationN;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.Test;

public class TabledSourceTest {

	private final RelationN person = RelationN.of("person",
			Property.of("id"), Property.of("name"));

	private final Database db = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					person.apply(1L, "Ada"),
					person.apply(2L, "Alan"),
					person.apply(3L, "Kurt")))
			.get();

	private final AtomicInteger hits = new AtomicInteger();

	/** The reference database as a producer, counting every fetch. */
	private AnswerSource counting() {
		return probe -> {
			hits.incrementAndGet();
			return db.answers(probe);
		};
	}

	/** A probe image: bound slots carry their value, nulls are free. */
	private Call<Relation> probe(Object... slots) {
		List<Object> members = new ArrayList<>();
		int frees = 0;
		for (Object slot : slots) {
			members.add(slot == null ? Any.of(frees++) : lval(slot));
		}
		return Call.of(person, (Reified<?>) lval(Array.ofAll(members)));
	}

	/** Drive produce to completion, collecting the emissions. */
	private static List<Tuple2<Reified<?>, Condition>> drain(TabledSource source, Call<Relation> probe) {
		List<Tuple2<Reified<?>, Condition>> collected = new ArrayList<>();
		new BreadthFirstScheduler<>(source.produce(probe, answer -> {
			collected.add(answer);
			return Fiber.done(Nothing.nothing());
		})).get();
		return collected;
	}

	@Test
	public void probesMemoizeThroughTheOwnedTable() {
		TabledSource source = TabledSource.over(counting());
		assertThat(drain(source, probe(null, null))).hasSize(3);
		assertThat(drain(source, probe(null, null))).hasSize(3);
		assertThat(hits.get())
				.describedAs("the second identical probe joins the sealed entry")
				.isEqualTo(1);
	}

	@Test
	public void aWideSealedEntryServesTheNarrowProbe() {
		// call subsumption at the data boundary: the narrow probe reads the
		// wide entry through consume's unification filter — exactly the
		// answers it asked for, and the wrapped source is never re-hit
		TabledSource source = TabledSource.over(counting());
		drain(source, probe(null, null));
		List<Tuple2<Reified<?>, Condition>> narrow = drain(source, probe(2L, null));
		assertThat(hits.get()).isEqualTo(1);
		assertThat(narrow).hasSize(1);
	}

	// ---- the point: the derived relation, memoized for inter-solve reuse ----

	private final AtomicInteger bodyRuns = new AtomicInteger();

	/** The derived relation: person, passed through a counted goal body. */
	private TabledSource derived() {
		return TabledSource.solving(args -> {
			bodyRuns.incrementAndGet();
			return RelationN.relation(db, person, args.toJavaArray(Unifiable[]::new));
		});
	}

	/** The exact answers for {@code out}, rendered and sorted (order is the scheduler's). */
	private static List<String> answers(Goal goal, Unifiable<?> out) {
		return goal.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	private List<String> names(AnswerSource source) {
		Unifiable<String> out = lvar();
		return RelationN.relation(source, person, lvar(), out)
				.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	private List<String> names(AnswerProducer producer) {
		Unifiable<String> out = lvar();
		return RelationN.relation(producer, person, lvar(), out)
				.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	public void aDerivedRelationAnswersLikeItsGoal() {
		assertThat(names(derived())).isEqualTo(names(db));
	}

	@Test
	public void theSecondSolveIsServedFromTheTable() {
		// the table outlives the solve: sealed entries are portable values,
		// so a later solve replays them and the goal never runs again
		TabledSource source = derived();
		List<String> first = names(source);
		List<String> second = names(source);
		assertThat(second).isEqualTo(first);
		assertThat(bodyRuns.get())
				.describedAs("the second solve is replay, not re-derivation")
				.isEqualTo(1);
	}

	@Test
	public void aConditionalDerivedAnswerImposesAtTheConsumer() {
		// the body binds nothing and forbids one value: the entry's single
		// answer is CONDITIONAL — free image, a nogood riding the condition —
		// and delivery restates it at the consumer, where it decides
		TabledSource guarded = TabledSource.solving(args ->
				exclude(((Unifiable<Object>) args.get(0)).unifies(2L)));
		Unifiable<Long> id = lvar();
		assertThat(answers(RelationN.relation(guarded, person, id, lvar())
				.and(id.unifies(1L)), id)).containsExactly("{1}");
		Unifiable<Long> refused = lvar();
		assertThat(answers(RelationN.relation(guarded, person, refused, lvar())
				.and(refused.unifies(2L)), refused)).isEmpty();
	}

	@Test
	public void anAscendedConditionDeliversOnlyConverged() {
		// the same free image succeeds under two guards, so the cell ASCENDS:
		// {≠2}, then {≠2}∨{≠3}. Delivery forks one branch per conjunct of
		// the JOINED condition — two at id=4 — and the log enumerates ascent
		// DELTAS, so each conjunct delivers exactly once whatever the
		// arrival order
		TabledSource guarded = TabledSource.solving(args ->
				exclude(((Unifiable<Object>) args.get(0)).unifies(2L))
						.or(exclude(((Unifiable<Object>) args.get(0)).unifies(3L))));
		Unifiable<Long> id = lvar();
		assertThat(answers(RelationN.relation(guarded, person, id, lvar())
				.and(id.unifies(4L)), id)).containsExactly("{4}", "{4}");
	}

	@Test
	public void anAbsorbedConditionIsNeverDelivered() {
		// one disjunct guards the image, the other admits it outright: the
		// conditions join to 1 by absorption, and finality means the
		// transient guarded arrival is never a delivery — one branch, not two
		TabledSource guarded = TabledSource.solving(args ->
				exclude(((Unifiable<Object>) args.get(0)).unifies(2L))
						.or(Goal.success()));
		Unifiable<Long> id = lvar();
		assertThat(answers(RelationN.relation(guarded, person, id, lvar())
				.and(id.unifies(4L)), id)).containsExactly("{4}");
	}

	/** Edges 1→2 and 2→3. */
	private static Goal edge(Unifiable<Long> x, Unifiable<Long> y) {
		return x.unifies(1L).and(y.unifies(2L))
				.or(x.unifies(2L).and(y.unifies(3L)));
	}

	@Test
	public void aRecursiveInnerTabledBodySeals() {
		// the body recurses through an inner tabled goal — transitive
		// closure over the edges. Completion detection is inherited with
		// the compressor: the derived entry SEALS instead of hanging, the
		// closure reaches 1→3, and the sealed table serves the sync face
		Tabled<Tuple2<Unifiable<Long>, Unifiable<Long>>> path =
				Tabling.defineRecursive(self -> pair -> pair.apply((x, y) ->
						edge(x, y)
								.or(defer(() -> {
									Unifiable<Long> z = lvar();
									return self.apply(Tuple.of(x, z)).and(edge(z, y));
								}))));
		TabledSource reach = TabledSource.solving(args ->
				path.apply(Tuple.of((Unifiable<Long>) args.get(0), (Unifiable<Long>) args.get(1))));
		Unifiable<Long> from = lvar();
		Unifiable<Long> to = lvar();
		assertThat(answers(RelationN.relation(reach, person, from, to)
				.and(from.unifies(1L)).and(to.unifies(3L)), from)).containsExactly("{1}");
		assertThat(drain(reach, probe(null, null))).hasSize(3);
	}

	@Test
	public void duplicateArrivalsFoldInTheCell() {
		// dedup is the cell join's own algebra: a duplicate arrival is an
		// inert fold — no log entry, no emission
		Tuple2<Reified<?>, Condition> row = db.answers(probe(2L, null)).iterator().next();
		AnswerSource stuttering = probe -> Arrays.asList(row, row, row);
		TabledSource source = TabledSource.over(stuttering);
		assertThat(drain(source, probe(2L, null))).hasSize(1);
	}
}
