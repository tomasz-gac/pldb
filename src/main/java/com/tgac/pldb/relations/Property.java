package com.tgac.pldb.relations;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Property<T> {
	private final String name;
	private final boolean indexed;

	public static <T> Property<T> of(String name) {
		return new Property<>(name, false);
	}

	public Property<T> indexed() {
		return new Property<T>(name, true);
	}

	@Override
	public String toString() {
		return name;
	}
}
