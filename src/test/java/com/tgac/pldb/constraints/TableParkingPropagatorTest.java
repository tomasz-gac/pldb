package com.tgac.pldb.constraints;

// ABOUTME: The parking posted table's receipts: ground rows match the sync oracle,
// ABOUTME: conditional rows impose at commit, Any rows admit everything and skip supports.

import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.store.Constraint;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import com.tgac.pldb.ImmutableDatabase;
import com.tgac.pldb.TabledSource;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.RelationN;
import com.tgac.pldb.relations.Relations;
import java.util.Arrays;
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

	/** The reference relation as a derived source: the goal body is the db lookup. */
	private static TabledSource derived() {
		return TabledSource.solving(args ->
				RelationN.relation(db, r, args.toJavaArray(Unifiable[]::new)));
	}

	/** The parking post through the door: the producer overload resolves. */
	private static Goal posted(TabledSource source, Unifiable<?>... args) {
		return RelationN.posted(source, r, args);
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
		assertThat(posted(derived(), lval(2), lval("b")).solve(lvar()).count()).isEqualTo(1);
		assertThat(posted(derived(), lval(2), lval("c")).solve(lvar()).count()).isZero();
	}

	@Test
	public void anEmptyExtensionFails() {
		assertThat(posted(derived(), lval(9), lvar()).solve(lvar()).count()).isZero();
	}

	@Test
	public void aSingletonCandidateCollapsesToBindings() {
		Unifiable<String> viaParking = lvar();
		Unifiable<String> viaSync = lvar();
		assertThat(posted(derived(), lval(2), viaParking)
				.solve(viaParking)
				.map(Object::toString)
				.collect(Collectors.toList()))
				.isEqualTo(r.posted(db, lval(2), viaSync)
						.solve(viaSync)
						.map(Object::toString)
						.collect(Collectors.toList()));
	}

	@Test
	public void aGroundProbeReDerivesAndGuardsEvaluateAtTheSource() {
		// the body runs FROM THE KEY: a fresh ground probe re-derives with
		// ground args, so the guard evaluates at production — trivially true
		// at 1 (condition ONE, subsumed), a failed branch at 2 (empty
		// extension, fail). The commit path is for REPLAYED conditions
		TabledSource guarded = TabledSource.solving(args ->
				exclude(((Unifiable<Object>) args.get(0)).unifies(2)));
		assertThat(posted(guarded, lval(1), lval("x")).solve(lvar()).count()).isEqualTo(1);
		assertThat(posted(guarded, lval(2), lval("x")).solve(lvar()).count()).isZero();
	}

	@Test
	public void aReplayedConditionImposesAtCommit() {
		// the wide probe seals the entry with its CONDITIONAL answer; the
		// later ground probes replay it — the condition is not re-derived,
		// it is imposed by the discharge restate, and decides at the anchor
		TabledSource guarded = TabledSource.solving(args ->
				exclude(((Unifiable<Object>) args.get(0)).unifies(2)));
		assertThat(posted(guarded, lvar(), lvar()).solve(lvar()).count()).isEqualTo(1);
		assertThat(posted(guarded, lval(1), lval("x")).solve(lvar()).count()).isEqualTo(1);
		assertThat(posted(guarded, lval(2), lval("x")).solve(lvar()).count()).isZero();
	}

	@Test
	public void aMultiConjunctDischargeForksTheConde() {
		// two guards on the same free image fold to {≠2}⊕{≠3}. At the FREE
		// post the discharge forks one branch per conjunct — two conditional
		// answers. At a GROUND probe the replayed conjuncts restate at the
		// ground anchor during delivery: a satisfied guard discharges and the
		// re-captured answer is UNCONDITIONAL, so the branches fold to one
		// entry — one answer at 4 (both guards pass, folded), one at 2 (only
		// {≠3} survives delivery)
		TabledSource guarded = TabledSource.solving(args ->
				exclude(((Unifiable<Object>) args.get(0)).unifies(2))
						.or(exclude(((Unifiable<Object>) args.get(0)).unifies(3))));
		assertThat(posted(guarded, lvar(), lvar()).solve(lvar()).count()).isEqualTo(2);
		assertThat(posted(guarded, lval(4), lval("x")).solve(lvar()).count()).isEqualTo(1);
		assertThat(posted(guarded, lval(2), lval("x")).solve(lvar()).count()).isEqualTo(1);
	}

	@Test
	public void aWideRowDischargesByRestateLeavingTheColumnFree() {
		// the body pins the item and says nothing about the tag: the entry is
		// (1, Any) — commit unifies the item and couples the tag to a fresh
		// existential, so the item is decided and the tag stays open
		TabledSource wide = TabledSource.solving(args ->
				((Unifiable<Object>) args.get(0)).unifies(1));
		Unifiable<Integer> x = lvar();
		assertThat(posted(wide, x, lvar())
				.and(x.unifies(1))
				.solve(x).count()).isEqualTo(1);
		Unifiable<Integer> refused = lvar();
		assertThat(posted(wide, refused, lvar())
				.and(refused.unifies(2))
				.solve(refused).count()).isZero();
	}

	@Test
	public void anAnyColumnProjectsToTopAndStoresNoSupport() {
		// two live entries, one leaving the tag free: the tag column projects
		// to TOP — no support may be stored, or values the Any-row admits
		// would be wrongly pruned. Asserted mid-solve; the deliberate failure
		// keeps reify (and enforcement, S3's stage) out of this receipt
		TabledSource mixed = TabledSource.solving(args ->
				((Unifiable<Object>) args.get(0)).unifies(7)
						.or(((Unifiable<Object>) args.get(0)).unifies(8)
								.and(((Unifiable<Object>) args.get(1)).unifies("a"))));
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		assertThat(posted(mixed, x, y)
				.and(probe(p -> {
					Theory<TableConstraints> live =
							Constraint.in(p, TableConstraints.class).get().getTheory();
					assertThat(TableConstraints.empty().getValue(live, p.walk(y)).isDefined())
							.describedAs("an Any column must not store a support")
							.isFalse();
				}))
				.and(Goal.failure())
				.solve(x).count()).isZero();
	}
}
