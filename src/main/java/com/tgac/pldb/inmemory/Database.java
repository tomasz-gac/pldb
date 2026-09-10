package com.tgac.pldb.inmemory;

import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface Database extends AnswerSource {

	Try<Database> withFacts(List<Fact> facts);

	/** The API face: literals in, the door converts — a hole refuses by column. */
	default Try<Database> withFacts(Literal... rows) {
		return withFacts(Arrays.stream(rows).map(Literal::fact).collect(Collectors.toList()));
	}

	Try<Database> withoutFacts(List<Fact> facts);

	default Try<Database> withoutFacts(Literal... rows) {
		return withoutFacts(Arrays.stream(rows).map(Literal::fact).collect(Collectors.toList()));
	}

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
