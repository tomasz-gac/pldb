package com.tgac.pldb.sql;

// ABOUTME: The per-source codec registry: builtins pass through, registered types
// ABOUTME: translate both ways, the unknown refuses loudly on the write side.

import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * The value↔wire translation table, mirroring the compiler registry:
 * builtin Java types are wired at construction, user types register
 * before the source's first use. WRITES are strict — a value whose type
 * has no codec refuses by relation and column (and a structural value
 * keeps the modelling refusal: its relational spelling is a child
 * relation). READS are lenient — a cell whose type claims no codec
 * passes through unchanged, because the backend's types are the
 * backend's business and an opaque value still unifies as itself.
 */
public final class Codecs {

	private final Map<Class<?>, Codec<?>> byValue = new HashMap<>();
	private final Map<Class<?>, Codec<?>> byJdbc = new HashMap<>();

	private Codecs() {
	}

	/** Identity codecs for the JDBC-representable builtins. */
	public static Codecs builtin() {
		Codecs codecs = new Codecs();
		for (Class<?> type : new Class<?>[]{
				Integer.class, Long.class, Short.class, Byte.class,
				Double.class, Float.class, BigDecimal.class, BigInteger.class,
				Boolean.class, Character.class, String.class}) {
			codecs.identity(type);
		}
		return codecs;
	}

	@SuppressWarnings("unchecked")
	private <T> void identity(Class<T> type) {
		byValue.put(type, Codec.of(type, (Class<Object>) type, value -> value, cell -> (T) cell));
	}

	/** Registers both directions; a doubly-claimed type refuses. */
	public Codecs codec(Codec<?> codec) {
		if (byValue.containsKey(codec.getValueType())) {
			throw new IllegalStateException(
					"a codec for " + codec.getValueType().getSimpleName() + " is already registered");
		}
		if (byJdbc.containsKey(codec.getJdbcType())) {
			throw new IllegalStateException(
					"the wire type " + codec.getJdbcType().getSimpleName() + " is already claimed");
		}
		byValue.put(codec.getValueType(), codec);
		byJdbc.put(codec.getJdbcType(), codec);
		return this;
	}

	/** The write side: strict — refuses by relation and column. */
	Object encode(Relation relation, Property<?> column, Object value) {
		Codec<?> codec = byValue.get(value.getClass());
		if (codec != null) {
			return codec.encode(value);
		}
		if (value instanceof Iterable || value instanceof Map || value.getClass().isArray()) {
			throw new IllegalStateException("flush of " + relation.getName()
					+ ": column '" + column.getName() + "' holds a structural value ("
					+ value.getClass().getSimpleName()
					+ ") — a first-normal-form violation; model it as a child relation");
		}
		throw new IllegalStateException("flush of " + relation.getName()
				+ ": column '" + column.getName() + "' holds a "
				+ value.getClass().getSimpleName()
				+ " and no codec is registered for it");
	}

	/** The read side: lenient — an unclaimed cell type passes through. */
	Object decode(Object cell) {
		Codec<?> codec = byJdbc.get(cell.getClass());
		return codec == null ? cell : codec.decode(cell);
	}
}
