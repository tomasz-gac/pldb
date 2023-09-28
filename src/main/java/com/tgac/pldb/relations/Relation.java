package com.tgac.pldb.relations;

import java.util.Arrays;
import java.util.stream.Collectors;
public interface Relation {
	String getName();

	Property<?>[] getArgs();

	default String getId() {
		return String.format("%s(%s)", getName(),
				Arrays.stream(getArgs())
						.map(Property::getName)
						.collect(Collectors.joining(", ")));
	}

	static RelationN of(String name, Property<?>... args) {
		return new RelationN(name, args);
	}
}
