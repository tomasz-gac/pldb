package com.tgac.pldb;

// ABOUTME: The table constraint (docs/design/table-constraints.md): posted lookups
// ABOUTME: narrow as domains — joins prune, singletons collapse, branch only at labelling.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.functional.monad.Cont;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.constraints.Support;
import com.tgac.pldb.constraints.TableConstraints;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.Tuple;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * A posted lookup is a DOMAIN over candidate rows, not an enumeration: bindings
 * re-narrow it through the index, two posted tables sharing a variable prune
 * each other through their column supports, a singleton candidate set collapses
 * to bindings without branching, and branching happens only at {@code labelo}
 * or at reify (enforce grounds surviving supports, the FD convention).
 */
public class TableConstraintsTest {

	private static final Property<Integer> item = Property.of("item");
	private static final Property<String> tag = Property.of("tag");
	private static final Property<String> label = Property.of("label");
	private static final Property<Integer> price = Property.of("price");

	private static final Relations._2<Integer, String> r =
			Relations.relation("r", item.indexed(), tag.indexed());
	private static final Relations._2<String, Integer> s =
			Relations.relation("s", label.indexed(), price.indexed());
	private static final Relations._2<Integer, String> t =
			Relations.relation("t", item.indexed(), tag.indexed());

	private static final Database db = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					r.fact(1, "a"),
					r.fact(2, "b"),
					r.fact(3, "c"),
					s.fact("a", 10),
					s.fact("b", 20),
					s.fact("d", 40),
					t.fact(7, "u"),
					t.fact(7, "v")))
			.get();

	/** A goal that runs assertions against the live package and succeeds. */
	private static Goal probe(Consumer<Package> check) {
		return p -> {
			check.accept(p);
			return Cont.just(p);
		};
	}

	@Test
	public void aJoinPrunesThroughSharedColumnSupportsWithoutBranching() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Unifiable<Integer> z = lvar();

		List<String> answers = r.posted(db, x, y)
				.and(s.posted(db, y, z))
				.and(probe(p -> {
					TableConstraints store = p.getStore(TableConstraints.class);
					// y is the SHARED column: r's tags met with s's labels,
					// {a,b,c} ∧ {a,b,d} = {a,b} — the only support materialized
					assertThat(store.getValue(p.walk(y)).get())
							.isEqualTo(Support.of("a", "b"));
					// x and z are lone columns: their projections are transient,
					// nobody reads them, nothing is stored
					assertThat(store.getValue(p.walk(x)).isDefined()).isFalse();
					assertThat(store.getValue(p.walk(z)).isDefined()).isFalse();
				}))
				.solve(lval(Tuple.of(x, y, z)))
				.map(Term::get)
				.map(t -> t._1.get() + "," + t._2.get() + "," + t._3.get())
				.collect(Collectors.toList());

		assertThat(answers).containsExactlyInAnyOrder("1,a,10", "2,b,20");
	}

	@Test
	public void aSingletonCandidateSetCollapsesToBindingsWithoutBranching() {
		Unifiable<String> y = lvar();

		long count = r.posted(db, lval(2), y)
				.and(probe(p -> assertThat(p.walk(y).get()).isEqualTo("b")))
				.solve(y)
				.count();
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void bindingAfterPostingWakesTheTableAndCollapses() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();

		long count = r.posted(db, x, y)
				.and(x.unifies(3))
				.and(probe(p -> assertThat(p.walk(y).get()).isEqualTo("c")))
				.solve(y)
				.count();
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void anEmptyCandidateSetFails() {
		Unifiable<String> y = lvar();
		assertThat(r.posted(db, lval(99), y).solve(y).count()).isZero();
	}

	@Test
	public void aGroundPostIsAMembershipCheck() {
		Unifiable<String> out = lvar();
		assertThat(r.posted(db, lval(1), lval("a")).and(out.unifies("yes")).solve(out).count())
				.isEqualTo(1);
		assertThat(r.posted(db, lval(1), lval("b")).and(out.unifies("yes")).solve(out).count())
				.isZero();
	}

	@Test
	public void labeloBranchesOverTheLiveSupport() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();

		// labelling the SHARED column: each y branch collapses both records
		List<Integer> items = r.posted(db, x, y)
				.and(s.posted(db, y, lvar()))
				.and(TableConstraints.labelo(y))
				.solve(x)
				.map(Term::get)
				.collect(Collectors.toList());
		assertThat(items).containsExactlyInAnyOrder(1, 2);
	}

	@Test
	public void aLoneTableStoresNoSupports() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();

		long count = r.posted(db, x, y)
				.and(probe(p -> {
					TableConstraints store = p.getStore(TableConstraints.class);
					assertThat(store.getValue(p.walk(x)).isDefined()).isFalse();
					assertThat(store.getValue(p.walk(y)).isDefined()).isFalse();
				}))
				.and(x.unifies(1))
				.and(y.unifies("a"))
				.solve(y)
				.count();
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void anUnresolvedLoneRecordRefusesAtReify() {
		// a lone record's projections are transient, so nothing can ground it
		// at reify — refuse loudly; the row-labeller is exists on the same args
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();

		assertThatThrownBy(() ->
						r.posted(db, x, y).solve(y).count())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("without domain");
	}

	@Test
	public void aLoneColumnCollapseStillBindsTransiently() {
		// both rows of t agree on item — the singleton projection binds x
		// without ever storing a support
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();

		long count = t.posted(db, x, y)
				.and(probe(p -> {
					assertThat(p.walk(x).get()).isEqualTo(7);
					TableConstraints store = p.getStore(TableConstraints.class);
					assertThat(store.getValue(p.walk(y)).isDefined()).isFalse();
				}))
				.and(y.unifies("u"))
				.solve(y)
				.count();
		assertThat(count).isEqualTo(1);
	}

	@Test
	public void lateAliasingMaterializesTheSharedColumn() {
		Unifiable<Integer> x = lvar();
		Unifiable<String> y = lvar();
		Unifiable<String> l = lvar();
		Unifiable<Integer> z = lvar();

		// posted apart: no sharing, nothing stored; the alias welds y~l and
		// both records materialize the column and meet
		long count = r.posted(db, x, y)
				.and(s.posted(db, l, z))
				.and(probe(p -> {
					TableConstraints store = p.getStore(TableConstraints.class);
					assertThat(store.getValue(p.walk(y)).isDefined()).isFalse();
					assertThat(store.getValue(p.walk(l)).isDefined()).isFalse();
				}))
				.and(y.unifies(l))
				.and(probe(p -> {
					TableConstraints store = p.getStore(TableConstraints.class);
					assertThat(store.getValue(p.walk(y)).get())
							.isEqualTo(Support.of("a", "b"));
				}))
				.solve(lval(Tuple.of(x, y, z)))
				.count();
		assertThat(count).isEqualTo(2);
	}

	@Test
	public void postedAgreesWithExists() {
		Unifiable<Integer> x1 = lvar();
		Unifiable<String> y1 = lvar();
		Unifiable<Integer> z1 = lvar();
		List<String> viaExists = r.exists(db, x1, y1)
				.and(s.exists(db, y1, z1))
				.solve(lval(Tuple.of(x1, y1, z1)))
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());

		Unifiable<Integer> x2 = lvar();
		Unifiable<String> y2 = lvar();
		Unifiable<Integer> z2 = lvar();
		List<String> viaPosted = r.posted(db, x2, y2)
				.and(s.posted(db, y2, z2))
				.solve(lval(Tuple.of(x2, y2, z2)))
				.map(Object::toString)
				.sorted()
				.collect(Collectors.toList());

		assertThat(viaPosted).hasSize(2).isEqualTo(viaExists);
	}
}
