package com.tgac.pldb.constraints;

// ABOUTME: The parking posted table's receipts: ground rows match the sync oracle,
// ABOUTME: conditional rows impose at commit, Any rows admit everything and skip supports.

import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.fibers.schedulers.BreadthFirstScheduler;
import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.store.Constraint;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.Database;
import com.tgac.pldb.ImmutableDatabase;
import com.tgac.pldb.TabledSource;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.Test;

public class TableParkingPropagatorTest {

	private static final Property<Integer> item = Property.of("item");
	private static final Property<String> tag = Property.of("tag");

	private static final Relations._2<Integer, String> r =
			Relations.relation("r", item.indexed(), tag.indexed());

	private static final Database db = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					r.fact(1, "a"),
					r.fact(2, "b"),
					r.fact(3, "c")))
			.get();

	/** The reference relation derived: the typed body is the db lookup, no casts. */
	private static Relations._2<Integer, String>.Derived derived() {
		return r.solving((i, t) -> r.exists(db, i, t));
	}

	/** The exact answers for {@code out}, rendered and sorted (order is the scheduler's). */
	private static List<String> answers(Goal goal, Unifiable<?> out) {
		return goal.solve(out)
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());
	}

	/** A goal that runs assertions against the live package and succeeds. */
	private static Goal probe(Consumer<Package> check) {
		return p -> {
			check.accept(p);
			return Cont.just(p);
		};
	}

	@Test
	public void aGroundPostIsAMembershipCheck() {
		assertThat(answers(derived().posted(lval(2), lval("b")), lvar())).containsExactly("_.0");
		assertThat(answers(derived().posted(lval(2), lval("c")), lvar())).isEmpty();
	}

	@Test
	public void anEmptyExtensionFails() {
		assertThat(answers(derived().posted(lval(9), lvar()), lvar())).isEmpty();
	}

	@Test
	public void aSingletonCandidateCollapsesToBindings() {
		Unifiable<String> viaParking = lvar();
		Unifiable<String> viaSync = lvar();
		assertThat(answers(derived().posted(lval(2), viaParking), viaParking))
				.isEqualTo(answers(r.posted(db, lval(2), viaSync), viaSync))
				.containsExactly("{b}");
	}

	@Test
	public void theDerivedTableIsExposed() {
		// the owned table outlives the solve and the handle hands it out —
		// the warm-start and marshal door; production lands entries in it
		Relations._2<Integer, String>.Derived cached = derived();
		assertThat(cached.table().size()).isZero();
		answers(cached.posted(lval(2), lvar()), lvar());
		assertThat(cached.table().size()).isGreaterThan(0);
	}

	@Test
	public void aGroundProbeReDerivesAndGuardsEvaluateAtTheSource() {
		// the body runs FROM THE KEY: a fresh ground probe re-derives with
		// ground args, so the guard evaluates at production — trivially true
		// at 1 (condition ONE, subsumed), a failed branch at 2 (empty
		// extension, fail). The commit path is for REPLAYED conditions
		Relations._2<Integer, String>.Derived guarded =
				r.solving((i, t) -> exclude(i.unifies(2)));
		assertThat(answers(guarded.posted(lval(1), lval("x")), lvar())).containsExactly("_.0");
		assertThat(answers(guarded.posted(lval(2), lval("x")), lvar())).isEmpty();
	}

	@Test
	public void aReplayedConditionImposesAtCommit() {
		// the wide post discharges its one CONDITIONAL entry — the answer is
		// the watched arg under the guard, verbatim. The later ground probes
		// replay the sealed entry: the condition is not re-derived, it is
		// imposed by the discharge restate, and decides at the anchor
		Relations._2<Integer, String>.Derived guarded =
				r.solving((i, t) -> exclude(i.unifies(2)));
		Unifiable<Integer> x = lvar();
		assertThat(answers(guarded.posted(x, lvar()), x))
				.containsExactly("_.0 : ¬(_.0 ≡ {2})");
		assertThat(answers(guarded.posted(lval(1), lval("x")), lvar())).containsExactly("_.0");
		assertThat(answers(guarded.posted(lval(2), lval("x")), lvar())).isEmpty();
	}

	@Test
	public void aMultiConjunctDischargeForksTheConde() {
		// two guards on the same free image fold to {≠2}⊕{≠3}. The free post
		// discharges one branch per conjunct — the conditions attach to the
		// WATCHED arg (a fresh out var projects none of them and reads as a
		// bare _.0). At a GROUND probe the replayed conjuncts restate at the
		// ground anchor during delivery: a satisfied guard discharges and the
		// re-captured answer is UNCONDITIONAL, so the branches fold to one
		// entry — one answer at 4 (both guards pass), one at 2 ({≠3} survives)
		Relations._2<Integer, String>.Derived guarded =
				r.solving((i, t) -> exclude(i.unifies(2)).or(exclude(i.unifies(3))));
		Unifiable<Integer> x = lvar();
		assertThat(answers(guarded.posted(x, lvar()), x))
				.containsExactly("_.0 : ¬(_.0 ≡ {2})", "_.0 : ¬(_.0 ≡ {3})");
		assertThat(answers(guarded.posted(lval(4), lval("x")), lvar())).containsExactly("_.0");
		assertThat(answers(guarded.posted(lval(2), lval("x")), lvar())).containsExactly("_.0");
	}

	@Test
	public void aWideRowDischargesByRestateLeavingTheColumnFree() {
		// the body pins the item and says nothing about the tag: the entry is
		// (1, Any) — commit unifies the item and couples the tag to a fresh
		// existential, so the item is decided and the tag stays open
		Relations._2<Integer, String>.Derived wide =
				r.solving((i, t) -> i.unifies(1));
		Unifiable<Integer> x = lvar();
		assertThat(answers(wide.posted(x, lvar()), x)).containsExactly("{1}");
		Unifiable<String> tagFree = lvar();
		assertThat(answers(wide.posted(lvar(), tagFree), tagFree)).containsExactly("_.0");
	}

	@Test
	public void aCoupledRowRefusesMismatchedGroundArgs() {
		// the body couples the columns: the one entry is (Any₀, Any₀) — the
		// diagonal, not the plane. A ground post off the diagonal must fail;
		// on it, discharge — never subsumed on the uncoupled shadow
		Relations._2<String, String> pair = Relations.relation("pair",
				Property.of("l"), Property.of("r"));
		Relations._2<String, String>.Derived diagonal =
				pair.solving((l, rr) -> l.unifies(rr));
		assertThat(answers(diagonal.posted(lval("v"), lval("v")), lvar())).containsExactly("_.0");
		assertThat(answers(diagonal.posted(lval("v"), lval("w")), lvar())).isEmpty();
		// and through a sealed WIDE entry: the coupling rides the replay —
		// consume's unification filter kills the off-diagonal delivery
		Relations._2<String, String>.Derived replayed =
				pair.solving((l, rr) -> l.unifies(rr));
		assertThat(answers(replayed.posted(lvar(), lvar()), lvar())).containsExactly("_.0");
		assertThat(answers(replayed.posted(lval("v"), lval("w")), lvar())).isEmpty();
		assertThat(answers(replayed.posted(lval("v"), lval("v")), lvar())).containsExactly("_.0");
	}

	@Test
	public void anEntailedRowDischargesTheAlternatives() {
		// one wide row at ONE entails every tuple: the disjunct absorbs the
		// whole ⊕ (1 ⊕ a = 1), so the constraint dissolves — alternatives
		// and all — instead of parking on the choice between two rows
		Relations._2<Integer, String>.Derived tautology =
				r.solving((i, t) -> Goal.success().or(i.unifies(1).and(t.unifies("b"))));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(answers(tautology.posted(x, y), lvar())).containsExactly("_.0");
	}

	@Test
	public void aParkedTableGroundsRowWiseAtReify() {
		// a record still parked at reify grounds ROW-WISE through enforce —
		// the parking kind's leg of the sync discipline, never a cartesian
		Unifiable<Integer> x = lvar();
		assertThat(answers(derived().posted(x, lvar()), x))
				.containsExactly("{1}", "{2}", "{3}");
	}

	@Test
	public void postedAgreesWithExists() {
		// the arc's oracle: a posted derived relation answers exactly like
		// its enumerating goal — enforce grounds what labelling left open
		Unifiable<Integer> px = lvar();
		Unifiable<String> py = lvar();
		Unifiable<Integer> ex = lvar();
		Unifiable<String> ey = lvar();
		assertThat(answers(derived().posted(px, py), lval(Tuple.of(px, py))))
				.isEqualTo(answers(r.exists(db, ex, ey), lval(Tuple.of(ex, ey))));
	}

	@Test
	public void aWideRowGroundsLeavingItsColumnOpenAtReify() {
		// two rows, one wide: enforce branches row-wise; the wide branch
		// binds the item, couples the tag to a fresh existential, and the
		// re-woken record discharges by entailment — the tag stays open
		Relations._2<Integer, String>.Derived mixed =
				r.solving((i, t) -> i.unifies(7).or(i.unifies(8).and(t.unifies("a"))));
		Unifiable<Integer> x = lvar();
		assertThat(answers(mixed.posted(x, lvar()), x)).containsExactly("{7}", "{8}");
	}

	@Test
	public void aConditionalRowRidesItsConditionThroughReify() {
		// the guarded row's branch imposes its nogood; the re-woken record
		// discharges as the lone survivor and the condition reifies as the
		// answer's residue
		Relations._2<Integer, String>.Derived guarded =
				r.solving((i, t) -> i.unifies(7).and(exclude(t.unifies("q")))
						.or(i.unifies(8).and(t.unifies("a"))));
		Unifiable<String> y = lvar();
		assertThat(answers(guarded.posted(lvar(), y), y))
				.containsExactly("_.0 : ¬(_.0 ≡ {q})", "{a}");
	}

	@Test
	public void aSyncSourceShippingConditionsImposesAtCommit() {
		// the kinds are symmetric: a hand-rolled SYNC source serving a
		// conditional answer is consumed like the parking kind's — imposed
		// at commit, never refused. The answer is manufactured by draining
		// a derived produce once and canning the emission
		TabledSource producing = TabledSource.solving(args ->
				exclude(((Unifiable<Object>) args.get(0)).unifies(2)));
		Call<Relation> wide = Call.of(r, (Reified<?>) lval(Array.of(Any.of(0), Any.of(1))));
		List<Tuple2<Reified<?>, Condition>> canned = new ArrayList<>();
		new BreadthFirstScheduler<>(producing.produce(wide, answer -> {
			canned.add(answer);
			return Fiber.done(Nothing.nothing());
		})).get();
		AnswerSource sync = probe -> canned;
		Unifiable<Integer> x = lvar();
		assertThat(answers(r.posted(sync, x, lvar()), x))
				.containsExactly("_.0 : ¬(_.0 ≡ {2})");
		assertThat(answers(r.posted(sync, lval(2), lval("q")), lvar())).isEmpty();
		assertThat(answers(r.posted(sync, lval(1), lval("q")), lvar())).containsExactly("_.0");
	}

	@Test
	public void anAnyColumnProjectsToTopAndStoresNoSupport() {
		// two live entries, one leaving the tag free: the tag column projects
		// to TOP — no support may be stored, or values the Any-row admits
		// would be wrongly pruned. Asserted mid-solve; the deliberate failure
		// keeps reify (and enforcement, S3's stage) out of this receipt
		Relations._2<Integer, String>.Derived mixed =
				r.solving((i, t) -> i.unifies(7).or(i.unifies(8).and(t.unifies("a"))));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(answers(mixed.posted(x, y)
				.and(probe(p -> {
					Theory<TableConstraints> live =
							Constraint.in(p, TableConstraints.class).get().getTheory();
					assertThat(TableConstraints.empty().getValue(live, p.walk(y)).isDefined())
							.describedAs("an Any column must not store a support")
							.isFalse();
				}))
				.and(Goal.failure()), x)).isEmpty();
	}
}
