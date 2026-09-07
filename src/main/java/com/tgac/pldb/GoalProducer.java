package com.tgac.pldb;

// ABOUTME: An AnswerProducer that drives a goal into an injected Table — the one
// ABOUTME: produce bridge: rule extensions for the constraint side, table supplied
// ABOUTME: by whoever owns the residence.

import static com.tgac.logic.unification.LVal.lval;

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
import com.tgac.logic.tabling.TableEntry;
import com.tgac.logic.tabling.Tabling;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * The produce face of a rule over an INJECTED table: the anchor is the
 * captured heads — the probe image restates onto the variables the body
 * speaks, the tabled call keys off their resulting bindings (alpha-aligned
 * with goal-side consumption at the same probe), and deliveries image back
 * over them; sound on the clean package because its lineage is disjoint.
 * Residence is the injector's decision: hand in the solve's table and the
 * entry this production fills is the same one every goal-side consumer
 * reads — one production, two readings.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class GoalProducer implements AnswerProducer {

	private final Relation rel;
	private final Goal rule;
	private final Array<Unifiable<?>> heads;
	private final Table table;

	public static GoalProducer of(Relation rel, Goal rule, Array<Unifiable<?>> heads, Table table) {
		return new GoalProducer(rel, rule, heads, table);
	}

	/** The rule this producer drives — the literal's native reading takes it. */
	public Goal getRule() {
		return rule;
	}

	/** The variables the rule speaks — valid for native reading only over these. */
	public Array<Unifiable<?>> getHeads() {
		return heads;
	}

	@Override
	public Fiber<Nothing> produce(Call<Relation> probe, Emitter<Tuple2<Reified<?>, Condition>> emit) {
		Unifiable<Object> anchor = lval(heads.map(Unifiable::getObjectUnifiable));
		Goal seeded = Conjunction.of(
				Residues.restate(probe.getArguments(), probe.getResidues(), anchor),
				Tabling.call(rel, heads.map(Unifiable::getObjectUnifiable), () -> rule));
		return seeded.apply(Package.empty().withStore(table)).apply(answerPkg ->
				Residues.all(answerPkg, anchor).flatMap(answer ->
						emit.emit(Tuple.of(answer._1, Condition.of(answer._2)))));
	}

	@Override
	public long estimate(Call<Relation> probe) {
		TableEntry<Object> sealed = table.findSealedSubsumer(
				Call.of(rel, probe.getArguments(), probe.getResidues()));
		return sealed != null ? sealed.getAnswerCount() : Long.MAX_VALUE;
	}

	@Override
	public String id() {
		return "rule:" + rel.getName();
	}

	@Override
	public String toString() {
		return id();
	}
}
