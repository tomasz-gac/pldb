package com.tgac.pldb.inmemory;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.collection.IndexedSeq;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public interface Database extends AnswerSource {
	/**
	 * The schema-layer read: facts matching a bound pattern — one
	 * {@code Optional} per argument position, present where bound. What
	 * triggers, integrity constraints and the GAC tier consume directly.
	 */
	// TODO : remove in favor of AnswerSource.answers
	Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args);

	@Override
	default Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		List<Tuple2<Reified<?>, Condition>> rows = new ArrayList<>();
		for (Fact fact : get(probe.getRelation(), Answers.pattern(probe.getArguments()))) {
			rows.add(Answers.answer(fact));
		}
		return rows;
	}

	@Override
	default long estimate(Call<Relation> probe) {
		Iterable<Fact> bucket = get(probe.getRelation(), Answers.pattern(probe.getArguments()));
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
