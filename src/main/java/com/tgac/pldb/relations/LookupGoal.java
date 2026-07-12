package com.tgac.pldb.relations;

// ABOUTME: A relation lookup as a data goal: planner-visible, priced by its
// ABOUTME: index bucket — the Bounded citizen that collapses the pldb planner to data.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.monad.Cont;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.goals.optimizer.Bounded;
import com.tgac.logic.unification.LVal;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import io.vavr.control.Option;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * The lookup captures its own database, so cost estimates need no context:
 * {@link #answers} counts the index bucket the probe would hit under the
 * current bindings — the order function of the narrowing/widening taxonomy
 * (logic's docs/design/optimizer.md §3-4).
 */
@Value
@RequiredArgsConstructor(staticName = "of")
public class LookupGoal implements Goal, Bounded {
	Database db;
	Relation rel;
	Array<Unifiable<?>> args;

	@Override
	public Cont<Package, Nothing> apply(Package s) {
		return Cont.defer(() -> substituteQueryItems(s.substitution(), args)
				.map(q -> unifyQueryResults(q).apply(s)));
	}

	@Override
	public long answers(Substitutions s) {
		IndexedSeq<Optional<Object>> probe = args
				.map(u -> (Unifiable<?>) s.walk(u))
				.map(Unifiable::getObjectUnifiable)
				.map(Unifiable::asVal)
				.map(Option::toJavaOptional);
		return db.estimate(rel, probe);
	}

	private static Fiber<Array<Unifiable<?>>> substituteQueryItems(Substitutions s, Array<Unifiable<?>> query) {
		return query.toJavaStream()
				.map(u -> (Unifiable<?>) s.walk(u))
				.map(Stream::of)
				.map(Fiber::done)
				.reduce((acc, c) -> Fiber.zip(acc, c).map(t -> t.apply(Stream::concat)))
				.orElseGet(() -> Fiber.done(Stream.empty()))
				.map(q -> q.collect(Array.collector()));
	}

	private Goal unifyQueryResults(Array<Unifiable<?>> query) {
		return StreamSupport.stream(db.get(rel, query
								.map(Unifiable::getObjectUnifiable)
								.map(Unifiable::asVal)
								.map(Option::toJavaOptional))
						.spliterator(), false)
				.map(fact -> lval(fact.getValues().map(Object.class::cast).map(LVal::lval))
						.unifies(query.map(Unifiable::getObjectUnifiable)))
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
	}

	@Override
	public String toString() {
		return rel.toString() + args;
	}
}
