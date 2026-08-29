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
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerProducer;
import com.tgac.pldb.AnswerSource;
import io.vavr.Tuple2;
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
 * minted at the one reification site ({@link Residues#about}). Delivery is
 * uniform across kinds and conditions: each answer forks per condition
 * conjunct, and one conjunct's delivery is {@link Residues#restate} at the
 * query anchor — the image half unifies the row, the factor half imposes
 * the constraints; a ground row is the corner where the condition is ONE
 * and restate is pure unification. A source that is also the ASYNC kind
 * ({@link AnswerProducer}) streams: emissions deliver as the cell grows,
 * and the seal ends the branch.
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
				.flatMap(q -> {
					Unifiable<?> anchor = lval(q.map(Unifiable::getObjectUnifiable));
					return Residues.about(s, anchor)
							.map(key -> dispatch(Call.of(rel, key._1, key._2), anchor).apply(s));
				}));
	}

	/** Async sources stream through produce; sync sources enumerate inline. */
	private Goal dispatch(Call<Relation> probe, Unifiable<?> anchor) {
		if (source instanceof AnswerProducer) {
			AnswerProducer producer = (AnswerProducer) source;
			return st -> k -> producer.produce(probe,
					answer -> deliver(answer, anchor).apply(st).apply(k));
		}
		return sync(probe, anchor);
	}

	private Goal sync(Call<Relation> probe, Unifiable<?> anchor) {
		return StreamSupport.stream(source.answers(probe).spliterator(), false)
				.flatMap(answer -> answer._2.conjuncts().toJavaStream()
						.map(conjunct -> (Goal) Residues.restate(answer._1, conjunct, anchor)))
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
	}

	/** One answer, forked per condition conjunct, each restated at the anchor. */
	private static Goal deliver(Tuple2<Reified<?>, Condition> answer, Unifiable<?> anchor) {
		return answer._2.conjuncts().toJavaStream()
				.map(conjunct -> (Goal) Residues.restate(answer._1, conjunct, anchor))
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
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

	@Override
	public String toString() {
		return rel.toString() + args;
	}
}
