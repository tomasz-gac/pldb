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
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.unification.LVal;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import io.vavr.collection.Array;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * The lookup captures its own source, so cost estimates need no context:
 * {@link #answers} counts the index bucket the probe would hit under the
 * current bindings — the order function of the narrowing/widening taxonomy
 * (logic's docs/design/optimizer.md §3-4). The probe IS the call key,
 * minted at the one reification site ({@link Residues#about}): the walked
 * argument image plus each projecting store's knowledge about the call's
 * free vars.
 */
@Value
@RequiredArgsConstructor(staticName = "of")
public class LookupGoal implements Goal, Bounded {
	AnswerSource source;
	Relation rel;
	Array<Unifiable<?>> args;

	@Override
	public Cont<Package, Nothing> apply(Package s) {
		return Cont.defer(() -> substituteQueryItems(s.substitution(), args)
				.flatMap(q -> Residues.about(s, lval(q.map(Unifiable::getObjectUnifiable)))
						.map(key -> unifyAnswers(q, Call.of(rel, key._1, key._2)).apply(s))));
	}

	@Override
	public long answers(Substitutions s) {
		Reified<?> image = MiniKanren.reify(s,
						lval(args.map(u -> (Unifiable<?>) s.walk(u))
								.map(Unifiable::getObjectUnifiable))
								.getObjectTerm())
				.ground();
		return source.estimate(Call.of(rel, image));
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

	private Goal unifyAnswers(Array<Unifiable<?>> query, Call<Relation> probe) {
		return StreamSupport.stream(source.answers(probe).spliterator(), false)
				.map(answer -> {
					if (!Condition.ONE.equals(answer._2)) {
						// the lookup delivers ground rows; conditional answers
						// wait for the consumption that can impose them
						throw new IllegalStateException(
								"conditional answers are not yet consumed by lookups: " + answer);
					}
					return (Goal) lval(Answers.values(answer._1).map(LVal::lval))
							.unifies(query.map(Unifiable::getObjectUnifiable));
				})
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
	}

	@Override
	public String toString() {
		return rel.toString() + args;
	}
}
