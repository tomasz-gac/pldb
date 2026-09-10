package com.tgac.pldb.inmemory;

import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Fact;
import io.vavr.control.Try;
import java.util.List;

public interface Database extends AnswerSource {

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
