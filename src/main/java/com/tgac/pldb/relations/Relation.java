package com.tgac.pldb.relations;

import com.tgac.functional.reflection.Types;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;

import java.util.Arrays;
import java.util.Optional;
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

	default Optional<Integer> indexOf(Property<?> property) {
		return Stream.range(0, getArgs().length)
				.map(i -> Tuple.of(i, getArgs()[i]))
				.filter(t -> property.getName().equals(t._2.getName()))
				.headOption()
				.toJavaOptional()
				.map(Tuple2::_1);
	}

	static RelationN of(String name, Property<?>... args) {
		return new RelationN(name, args);
	}
}
