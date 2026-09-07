package com.tgac.pldb.relations;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RelationNTest {
	Property<String> P0 = Property.of("P0");
	Property<String> P1 = Property.of("P1");
	Property<String> P2 = Property.of("P2");
	Property<String> P3 = Property.of("P3");
	Property<String> P4 = Property.of("P4");
	Property<String> P5 = Property.of("P5");
	Property<String> P6 = Property.of("P6");
	Property<String> P7 = Property.of("P7");
	Property<Float> DUPLICATED = Property.of("P0");

	@Test
	public void shouldBuildValidRelation(){
		RelationN.of("test", P0);
		RelationN.of("test", P0, P1);
		RelationN.of("test", P0, P1, P2);
		RelationN.of("test", P0, P1, P2, P3);
		RelationN.of("test", P0, P1, P2, P3, P4);
		RelationN.of("test", P0, P1, P2, P3, P4, P5);
		RelationN.of("test", P0, P1, P2, P3, P4, P5, P6);
		RelationN.of("test", P0, P1, P2, P3, P4, P5, P6, P7);
	}

	@Test
	public void shouldThrowOnDuplicatedName(){
		Assertions.assertThatThrownBy(() ->
				RelationN.of("test", P0, DUPLICATED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
		Assertions.assertThatThrownBy(() ->
				RelationN.of("test", P0, P1, DUPLICATED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
		Assertions.assertThatThrownBy(() ->
				RelationN.of("test", P0, P1, P2, DUPLICATED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
		Assertions.assertThatThrownBy(() ->
				RelationN.of("test", P0, P1, P2, P3, DUPLICATED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
		Assertions.assertThatThrownBy(() ->
				RelationN.of("test", P0, P1, P2, P3, P4, DUPLICATED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
		Assertions.assertThatThrownBy(() ->
				RelationN.of("test", P0, P1, P2, P3, P4, P5, DUPLICATED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
		Assertions.assertThatThrownBy(() ->
				RelationN.of("test", P0, P1, P2, P3, P4, P5, P6, DUPLICATED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
		Assertions.assertThatThrownBy(() ->
				RelationN.of("test", P0, P1, P2, P3, P4, P5, P6, P7, DUPLICATED))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicated");
	}
}