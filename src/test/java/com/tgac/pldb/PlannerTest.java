package com.tgac.pldb;

// ABOUTME: Acceptance for the ordered planner: answers identical planned vs
// ABOUTME: unplanned, and a mis-ordered query enumerates an order of magnitude fewer facts.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.goals.optimizer.CascadingOptimizer;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import io.vavr.Tuple2;
import com.tgac.logic.goals.optimizer.Optimizer;
import com.tgac.logic.goals.optimizer.OrderingOptimizer;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.inmemory.Trigger;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.relations.RelationN;
import io.vavr.collection.IndexedSeq;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.Test;

public class PlannerTest {

	private static Literal parent(AnswerSource db, Unifiable<Integer> parentId, Unifiable<Integer> childId) {
		return Literal.relation("parentP")
				.arg("parentId", parentId).indexed()
				.arg("childId", childId).indexed()
				.from(db);
	}

	private static Relation parentRel() {
		return parent(null, lvar(), lvar()).getRel();
	}

	private static final int N = 40;
	private static final Property<Integer> parentId = Property.of("parentId");
	private static final Property<Integer> childId = Property.of("childId");


	/** Counts facts the index yields — the probe metric of query-planning.md §Phase 2. */
	private static final class CountingDb implements Database {
		private final Database inner;
		private final AtomicLong yielded;

		CountingDb(Database inner, AtomicLong yielded) {
			this.inner = inner;
			this.yielded = yielded;
		}

		@Override
		public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
			List<Tuple2<Reified<?>, Condition>> out = new ArrayList<>();
			inner.answers(probe).forEach(row -> {
				yielded.incrementAndGet();
				out.add(row);
			});
			return out;
		}

		@Override
		public long estimate(Call<Relation> probe) {
			// pricing is exempt from the enumeration metric: it measures the SEARCH
			return inner.estimate(probe);
		}

		@Override
		public Try<Database> withFacts(List<Fact> facts) {
			return inner.withFacts(facts).map(db -> new CountingDb(db, yielded));
		}

		@Override
		public Try<Database> withoutFacts(List<Fact> facts) {
			return inner.withoutFacts(facts).map(db -> new CountingDb(db, yielded));
		}

		@Override
		public Database withTrigger(Trigger trigger) {
			return new CountingDb(inner.withTrigger(trigger), yielded);
		}
	}

	private static Database chain(AtomicLong counter) {
		List<Fact> facts = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			facts.add(parent(null, lval(i), lval(i + 1)).fact());
		}
		return new CountingDb(ImmutableDatabase.empty(), counter).withFacts(facts).get();
	}

	/** grandparent-of-39, deliberately mis-ordered: the unbound joins first. */
	private static com.tgac.logic.goals.Goal misOrdered(Database db, Unifiable<Integer> gp) {
		Unifiable<Integer> p = lvar();
		return parent(db, gp, p)
				.and(parent(db, p, lval(N - 1)));
	}

	@Test
	public void plannedAnswersAreIdenticalAndEnumerateFarFewerFacts() {
		AtomicLong plain = new AtomicLong();
		Unifiable<Integer> gp1 = lvar();
		List<String> unplanned = misOrdered(chain(plain), gp1).solve(gp1)
				.map(Object::toString).collect(Collectors.toList());

		AtomicLong planned = new AtomicLong();
		Unifiable<Integer> gp2 = lvar();
		Optimizer pipeline = Optimizer.pipeline(new CascadingOptimizer(), new OrderingOptimizer());
		List<String> plannedAnswers = misOrdered(chain(planned), gp2).solve(gp2, pipeline)
				.map(Object::toString).collect(Collectors.toList());

		assertThat(plannedAnswers).isEqualTo(unplanned);
		assertThat(unplanned).containsExactly("{" + (N - 3) + "}");
		// unplanned: enumerate all N parent facts, then one indexed hit (N + 1);
		// planned: the bound lookup first — one bucket fact, then one more (2)
		assertThat(plain.get()).isGreaterThan((long) N);
		assertThat(planned.get() * 5).isLessThan(plain.get());
	}
}
