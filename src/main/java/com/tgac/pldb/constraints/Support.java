package com.tgac.pldb.constraints;

// ABOUTME: The column-support lattice: the finite set of values a column may still
// ABOUTME: take across a posted table's candidate rows. Meet is intersection.

import com.tgac.logic.lattice.Domain;
import io.vavr.collection.HashSet;
import io.vavr.control.Option;
import lombok.Value;

/**
 * A free column's possible values across the candidate rows ARE a finite
 * domain (docs/design/table-constraints.md §2): membership is the admission
 * check, a size-one support collapses to the binding, and two posted tables
 * sharing a variable prune each other by meeting their supports pointwise in
 * the store.
 */
@Value
public class Support implements Domain<Support> {

	HashSet<Object> values;

	public static Support of(Object... vs) {
		return new Support(HashSet.of(vs));
	}

	public static Support ofAll(Iterable<?> vs) {
		return new Support(HashSet.ofAll(vs));
	}

	@Override
	public Support meet(Support other) {
		return new Support(values.intersect(other.values));
	}

	@Override
	public boolean leq(Support other) {
		return other.values.containsAll(values);
	}

	@Override
	public boolean isBottom() {
		return values.isEmpty();
	}

	@Override
	public boolean admits(Object ground) {
		return values.contains(ground);
	}

	@Override
	public Option<Object> asPoint() {
		return values.size() == 1 ? Option.of(values.head()) : Option.none();
	}

	@Override
	public String toString() {
		return values.mkString("{", ",", "}");
	}
}
