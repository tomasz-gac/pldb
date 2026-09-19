package org.clauseway.pldb.inmemory;

import org.clauseway.logic.unification.Reified;
import org.clauseway.logic.unification.Term;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import java.util.HashSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ColumnIndex {
	/** Null is a legitimate bucket key; vavr maps want a witness for it. */
	private static final Object NULL_KEY = new Object();

	Map<Object, Set<Reified<?>>> buckets;
	Set<Reified<?>> wide;

	static ColumnIndex empty() {
		return new ColumnIndex(LinkedHashMap.empty(), LinkedHashSet.empty());
	}

	ColumnIndex with(Term<Object> cell, Reified<?> image) {
		if (isFree(cell)) {
			return new ColumnIndex(buckets, wide.add(image));
		}
		Object key = getKey(cell);
		Set<Reified<?>> bucket = buckets.getOrElse(key, LinkedHashSet.empty());
		return new ColumnIndex(buckets.put(key, bucket.add(image)), wide);
	}

	ColumnIndex without(Term<Object> cell, Reified<?> image) {
		if (isFree(cell)) {
			return new ColumnIndex(buckets, wide.remove(image));
		}
		Object key = getKey(cell);
		return new ColumnIndex(buckets.get(key)
				.map(bucket -> buckets.put(key, bucket.remove(image)))
				.getOrElse(buckets), wide);
	}

	/** The value's bucket plus every row free at this column, as mutable scratch. */
	HashSet<Reified<?>> matching(Object value) {
		Object key = value == null ? NULL_KEY : value;
		HashSet<Reified<?>> matched = new HashSet<>();
		buckets.get(key).forEach(bucket -> bucket.forEach(matched::add));
		wide.forEach(matched::add);
		return matched;
	}

	private static boolean isFree(Term<?> v) {
		return !v.asVal().isDefined();
	}

	private static Object getKey(Term<Object> cell) {
		return cell.get() == null ? NULL_KEY : cell.get();
	}
}
