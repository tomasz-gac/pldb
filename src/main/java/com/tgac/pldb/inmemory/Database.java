package com.tgac.pldb.inmemory;

import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.Writer;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Literal;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface Database extends AnswerSource, Writer<Database> {

	Try<Database> withFacts(List<Fact> facts);

	/** The API face: literals in, the door converts — a hole refuses by column. */
	@Override
	default Try<Database> withFacts(Collection<Literal> rows) {
		return withFacts(rows.stream().map(Literal::fact).collect(Collectors.toList()));
	}

	Try<Database> withoutFacts(List<Fact> facts);

	default Try<Database> withoutFacts(Collection<Literal> rows) {
		return withoutFacts(rows.stream().map(Literal::fact).collect(Collectors.toList()));
	}

	default Try<Database> withoutFacts(Literal... rows) {
		return withoutFacts(java.util.Arrays.asList(rows));
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
