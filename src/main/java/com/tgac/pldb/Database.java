package com.tgac.pldb;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.IndexedSeq;
import io.vavr.control.Try;

import java.util.List;
import java.util.Optional;
public interface Database {
	Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args);

	/**
	 * Upper bound on the facts {@link #get} would yield — the planner's order
	 * function (logic's optimizer.md §3). Exposure, not computation: the
	 * default counts, backends with sized buckets should override.
	 */
	default long estimate(Relation relation, IndexedSeq<Optional<Object>> args) {
		Iterable<Fact> bucket = get(relation, args);
		if (bucket instanceof java.util.Collection) {
			return ((java.util.Collection<?>) bucket).size();
		}
		long n = 0;
		for (@SuppressWarnings("unused") Fact f : bucket) {
			n++;
		}
		return n;
	}

	Try<Database> withFacts(List<Fact> facts);

	Try<Database> withoutFacts(List<Fact> facts);

	Database withTrigger(Trigger trigger);

	default Database withObserver(Observer observer) {
		return withTrigger((f, db) -> {
			observer.accept(f, db);
			return Try.success(db);
		});
	}

	default Database withConstraint(Constraint constraint) {
		return withTrigger((f, db) -> constraint.apply(f, db)
				.map(IllegalStateException::new)
				.map(Try::<Database>failure)
				.orElseGet(() -> Try.success(db)));
	}
}
