package com.tgac.pldb.transaction;

// ABOUTME: The composite pin's algebra: singletons lift, agreeing unions merge,
// ABOUTME: a region read at two pins refuses — staleness is structural, not silent.

import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.tabling.Call;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import lombok.Value;
import org.junit.Test;

public class FootprintTest {

	@Value
	private static class Generation implements Pin {
		int generation;
	}

	private static Call<Relation> region(String relation) {
		Relation rel = Literal.relation(FootprintTest.class, relation)
				.arg("id", lvar())
				.arg("value", lvar())
				.from(null)
				.getRel();
		Term<?>[] anys = new Term<?>[rel.getArgs().length];
		for (int i = 0; i < anys.length; i++) {
			anys[i] = Any.of(i);
		}
		return Call.of(rel, (Reified<?>) lval(Array.of(anys)));
	}

	@Test
	public void aFootprintIsAPinAndSingletonsLiftLeaves() {
		Footprint singleton = Footprint.of(region("person"), new Generation(1));
		assertThat(singleton).isInstanceOf(Pin.class);
		assertThat(Pinned.of("answers", singleton).getPin())
				.describedAs("a derived read completes with a composite pin")
				.isEqualTo(singleton);
	}

	@Test
	public void agreeingUnionsMergeAndDuplicateRegionsFold() {
		Footprint left = Footprint.of(region("person"), new Generation(1))
				.union(Footprint.of(region("book"), new Generation(4)));
		Footprint right = Footprint.of(region("person"), new Generation(1))
				.union(Footprint.of(region("loan"), new Generation(2)));

		Footprint union = left.union(right);
		assertThat(union.pins()).hasSize(3);
		assertThat(union.pins().get(region("person"))).isEqualTo(new Generation(1));
	}

	@Test
	public void aRegionReadAtTwoPinsRefusesTheUnion() {
		// the wire-face claim at unit grain: composing a part that read
		// person at world 1 with a part that read it at world 2 is a
		// STRUCTURAL conflict — never a silently stale answer
		Footprint atOne = Footprint.of(region("person"), new Generation(1));
		Footprint atTwo = Footprint.of(region("person"), new Generation(2));
		assertThatThrownBy(() -> atOne.union(atTwo))
				.isInstanceOf(Transaction.Conflict.class)
				.hasMessageContaining("person")
				.hasMessageContaining("different");
	}

	@Test
	public void emptyIsTheUnionIdentity() {
		Footprint some = Footprint.of(region("person"), new Generation(1));
		assertThat(Footprint.empty().union(some)).isEqualTo(some);
		assertThat(some.union(Footprint.empty())).isEqualTo(some);
	}
}
