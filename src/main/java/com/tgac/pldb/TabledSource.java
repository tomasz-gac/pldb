package com.tgac.pldb;

// ABOUTME: The table as the source: derived relations compressed by TABLING into an
// ABOUTME: owned table that outlives the solve — pldb translates probes and answers.

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
import com.tgac.logic.tabling.Tabled;
import com.tgac.logic.tabling.Tabling;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.control.Option;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The table as the source: a derived relation is a goal COMPRESSED by
 * tabling into answer cells, and this class owns the table the
 * compression lands in. Tabling takes its table from the package a call
 * runs in, so residence is a package, not a mechanism: {@link #produce}
 * applies the TABLED goal from {@code Package.empty().withStore(table)},
 * and the whole discipline is inherited rather than imitated — the
 * producer guards, claim-once mastery, finality (ground answers stream,
 * conditional answers deliver converged at the seal), consume's
 * unification filter (a narrow probe reads a sealed wide entry and gets
 * exactly the answers it asked for), and completion detection, so bodies
 * containing recursive tabled goals seal instead of hanging. The table
 * outlives any solve (sealed entries are portable values; validity over
 * time is the pins' job): {@link #solving} memoizes a DERIVED RELATION
 * for inter-solve reuse and, eventually, persistence — the entries are
 * what a memo store marshals.
 *
 * <p>{@link #produce} instantiates the probe's image into fresh
 * arguments, restates the probe's region onto them (so the key the
 * tabled call mints carries it), applies the goal, and re-captures each
 * delivery whole ({@link Residues#all}) for emission — the bridge
 * between the owned fixpoint and the consumer's state.
 *
 * <p>The tabled source is ONLY the async kind: consumption streams
 * through {@link #produce}, so the one sync consumer a data source has —
 * the posted table constraint — cannot take a tabled source by type
 * until it arrives as a PARKING propagator. Pricing translates the probe
 * to the tabled relation's key — same image, same region, its token —
 * and reads sealed entries.
 */
public final class TabledSource implements AnswerProducer {

	private final Table table = Table.empty();
	private final Function<Relation, Tabled<Array<Unifiable<?>>>> relations;
	private final Option<AnswerSource> backend;

	private TabledSource(Function<Relation, Tabled<Array<Unifiable<?>>>> relations,
			Option<AnswerSource> backend) {
		this.relations = relations;
		this.backend = backend;
	}

	/**
	 * Produce-on-miss over the sync backend: each relation gets its own
	 * tabled goal whose body enumerates the backend, so every probe lands
	 * in the owned table through the one compression path.
	 */
	public static TabledSource over(AnswerSource source) {
		Map<Relation, Tabled<Array<Unifiable<?>>>> defined = new ConcurrentHashMap<>();
		return new TabledSource(
				rel -> defined.computeIfAbsent(rel, r ->
						Tabling.define(args -> Literal.of(source, r, args))),
				Option.some(source));
	}

	/**
	 * The derived relation: a goal over positional arguments, defined ONCE
	 * as a tabled relation and memoized into the owned table. The body runs
	 * from the key, caller-agnostic; inner tabled calls accumulate beside
	 * the derived entries and seal under the same completion detection.
	 */
	public static TabledSource solving(Function<Array<Unifiable<?>>, Goal> body) {
		Tabled<Array<Unifiable<?>>> one = Tabling.define(body);
		return new TabledSource(rel -> one, Option.none());
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
					Goal call = relations.apply(probe.getRelation()).apply(args);
					Goal seeded = probe.getResidues().isTrue() ?
							call :
							Conjunction.of(
									Residues.restate(probe.getArguments(), probe.getResidues(), argsTerm),
									call);
					// not a grounding: the applied search IS produce's fiber —
					// successes leave through the emitter and completion is the
					// search's own exhaustion, the same shape as Tabling.produce;
					// Exhaustion.collected is for callers that must observe
					// exhaustion while capturing out-of-band
					return seeded.apply(Package.empty().withStore(table)).apply(answerPkg ->
							Residues.all(answerPkg, argsTerm).flatMap(answer ->
									emit.emit(Tuple.of(answer._1, Condition.of(answer._2)))));
				});
	}

	@Override
	public long estimate(Call<Relation> probe) {
		TableEntry<Object> sealed = sealedFor(probe);
		return sealed != null ? sealed.getAnswerCount()
				: backend.map(raw -> raw.estimate(probe)).getOrElse(Long.MAX_VALUE);
	}

	@Override
	public String id() {
		return backend.isDefined() ? backend.get().id() : AnswerProducer.super.id();
	}

	/**
	 * The owned table: sealed entries as portable values, what a memo store
	 * marshals and a warm start reconstructs. Validity over time is the
	 * pins' job, not the table's.
	 */
	public Table table() {
		return table;
	}

	/** The probe translated to the tabled relation's key: same image, same region, its token. */
	private TableEntry<Object> sealedFor(Call<Relation> probe) {
		return table.findSealedSubsumer(
				Call.of(relations.apply(probe.getRelation()), probe.getArguments(), probe.getResidues()));
	}

	@Override
	public String toString() {
		return "tabled(" + id() + ")";
	}
}
