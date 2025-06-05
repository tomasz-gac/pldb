package com.tgac.pldb;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.IndexedSeq;
import io.vavr.control.Try;

import java.util.List;
import java.util.Optional;
public interface Database {
	Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args);

	int count(Relation relation, IndexedSeq<Optional<Object>> args);

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
