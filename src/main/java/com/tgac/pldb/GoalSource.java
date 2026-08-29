package com.tgac.pldb;

// ABOUTME: The derived relation's producer: a goal run FROM THE KEY, caller-agnostic,
// ABOUTME: each answer captured whole (image + residues) and emitted as the cell's shape.

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Emitter;
import com.tgac.functional.fibers.Fiber;
import com.tgac.logic.goals.Conjunction;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.tabling.Table;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import java.util.function.Function;

/**
 * A goal as an {@link AnswerProducer} — the derived relation's workforce.
 * Production runs FROM THE KEY, the anonymous master's caller-agnostic
 * discipline: the probe's image instantiates into fresh variables, the
 * probe's region is restated onto them, and the body runs from an empty
 * package carrying the SHARED table — so inner tabled calls accumulate
 * beside the derived entries, and what the cell memoizes is exactly the
 * region the key names. Each answer is captured WHOLE ({@link Residues#all}
 * — the image with every projecting store's knowledge) and emitted as the
 * cell's own shape: conditional answers are the general case, ground rows
 * the corner where the condition is ONE.
 */
final class GoalSource implements AnswerProducer {

	private final Function<Array<Unifiable<?>>, Goal> body;
	private final Table shared;

	GoalSource(Function<Array<Unifiable<?>>, Goal> body, Table shared) {
		this.body = body;
		this.shared = shared;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Fiber<Nothing> produce(Call<Relation> probe, Emitter<Tuple2<Reified<?>, Condition>> emit) {
		return MiniKanren.instantiateWithAnys((Reified<Object>) probe.getArguments())
				.flatMap(instantiated -> {
					Unifiable<Object> argsTerm = instantiated._1;
					Array<Unifiable<?>> args = Array.ofAll(MiniKanren.members(argsTerm)
									.getOrElseThrow(() -> new IllegalArgumentException(
											"not a probe image: " + probe.getArguments())))
							.map(member -> (Unifiable<?>) member);
					Package from = Package.empty().withStore(shared);
					Goal seeded = probe.getResidues().isTrue() ?
							body.apply(args) :
							Conjunction.of(
									Residues.restate(probe.getArguments(), probe.getResidues(), argsTerm),
									body.apply(args));
					return seeded.apply(from).apply(answerPkg ->
							Residues.all(answerPkg, argsTerm).flatMap(answer ->
									emit.emit(Tuple.of(answer._1, Condition.of(answer._2)))));
				});
	}
}
