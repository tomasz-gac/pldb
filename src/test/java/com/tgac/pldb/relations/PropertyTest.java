package com.tgac.pldb.relations;

// ABOUTME: Property flags: ground() and indexed() are chainable metadata copies;
// ABOUTME: lookup identity stays the NAME, so flagged copies never break reads.

import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.vavr.collection.Array;
import org.junit.Test;

public class PropertyTest {

	@Test
	public void flagsDefaultToFalse() {
		Property<Integer> id = Property.of("id");
		assertThat(id.isIndexed()).isFalse();
		assertThat(id.isGround()).isFalse();
	}

	@Test
	public void flagsChainInEitherOrderAndPreserveEachOther() {
		Property<Integer> a = Property.<Integer> of("id").indexed().ground();
		assertThat(a.isIndexed()).isTrue();
		assertThat(a.isGround()).isTrue();

		Property<Integer> b = Property.<Integer> of("id").ground().indexed();
		assertThat(b.isIndexed()).isTrue();
		assertThat(b.isGround()).isTrue();
	}

	@Test
	public void lookupIdentityIsTheNameRegardlessOfFlags() {
		Property<Integer> id = Property.of("id");
		Relation rel = Literal.relation(PropertyTest.class, "r")
				.arg("id", lvar()).indexed().ground()
				.arg("tag", lvar())
				.from(null).getRel();

		// the bare constant finds the flagged copy's column
		assertThat(rel.indexOf(id)).contains(0);
		Fact fact = Fact.of(rel, Array.of(7, "a"));
		assertThat(fact.get(id)).contains(7);
	}

	@Test
	public void flaggedCopiesOfOneNameAreStillDuplicatesInARelation() {
		assertThatThrownBy(() -> Literal.relation(PropertyTest.class, "r")
				.arg("id", lvar()).indexed()
				.arg("id", lvar()).ground()
				.from(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
	}
}
