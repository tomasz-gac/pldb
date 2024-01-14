package com.tgac.pldb.relations;

import io.vavr.collection.Array;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Optional;

@Value
@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PUBLIC)
public class Fact {
	Relation relation;
	Array<?> values;

	@SuppressWarnings("unchecked")
	public <T> Optional<T> get(Property<T> property) {
		return relation.indexOf(property)
				.map(i -> getValues().get(i))
				.flatMap(v -> Try.of(() -> (T) v)
						.toJavaOptional());
	}
}