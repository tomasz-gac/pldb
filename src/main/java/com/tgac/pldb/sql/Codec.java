package com.tgac.pldb.sql;

// ABOUTME: One column type's two-way translation: a value flattens to a
// ABOUTME: JDBC-representable on write and re-forms from it on read.

import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * How a Java type crosses the JDBC boundary: {@link #encode} flattens a
 * value to its wire form on write and into bound probe parameters;
 * {@link #decode} re-forms it from the cells the driver returns. The
 * two class tokens key the registry in both directions, so one
 * registration serves flush, fetch and probes alike.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Codec<T> {

	private final Class<T> valueType;
	private final Class<?> jdbcType;
	private final Function<T, Object> encoder;
	private final Function<Object, T> decoder;

	public static <T, J> Codec<T> of(Class<T> valueType, Class<J> jdbcType,
			Function<T, J> encoder, Function<J, T> decoder) {
		return new Codec<>(valueType, jdbcType,
				value -> encoder.apply(value),
				cell -> decoder.apply(jdbcType.cast(cell)));
	}

	Object encode(Object value) {
		return encoder.apply(valueType.cast(value));
	}

	Object decode(Object cell) {
		return decoder.apply(cell);
	}
}
