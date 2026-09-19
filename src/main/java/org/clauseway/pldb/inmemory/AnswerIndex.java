package org.clauseway.pldb.inmemory;

import org.clauseway.logic.tabling.Call;
import org.clauseway.logic.tabling.Condition;
import org.clauseway.logic.unification.Reified;
import org.clauseway.logic.unification.Term;
import org.clauseway.pldb.relations.Answer;
import org.clauseway.pldb.relations.Answers;
import org.clauseway.pldb.relations.Relation;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class AnswerIndex {
	LinkedHashMap<Reified<?>, Condition> byImage;
	Map<Integer, ColumnIndex> byColumn;

	static AnswerIndex empty() {
		return new AnswerIndex(LinkedHashMap.empty(), HashMap.empty());
	}

	AnswerIndex with(Set<Integer> positions, Answer answer) {
		Reified<?> image = answer.getReified();
		Condition folded = byImage.get(image)
				.map(resident -> Condition.RING.plus(resident, answer.getCondition()))
				.getOrElse(answer.getCondition());
		if (byImage.containsKey(image)) {
			return new AnswerIndex(byImage.put(image, folded), byColumn);
		}
		Map<Integer, ColumnIndex> indexed = byColumn;
		Array<Term<Object>> cells = Answers.positions(image);
		for (int i = 0; i < cells.size(); i++) {
			if (!positions.contains(i)) {
				continue;
			}
			ColumnIndex column = indexed.getOrElse(i, ColumnIndex.empty());
			indexed = indexed.put(i, column.with(cells.get(i), image));
		}
		return new AnswerIndex(byImage.put(image, folded), indexed);
	}

	AnswerIndex without(Set<Integer> positions, Reified<?> image) {
		if (!byImage.containsKey(image)) {
			return this;
		}
		Map<Integer, ColumnIndex> indexed = byColumn;
		Array<Term<Object>> cells = Answers.positions(image);
		for (int i = 0; i < cells.size(); i++) {
			if (!positions.contains(i)) {
				continue;
			}
			ColumnIndex column = indexed.getOrElse(i, null);
			if (column != null) {
				indexed = indexed.put(i, column.without(cells.get(i), image));
			}
		}
		return new AnswerIndex(byImage.remove(image), indexed);
	}

	Iterable<Answer> answers(Relation relation, Set<Integer> positions, Call<?> probe) {
		Array<Term<Object>> args = Answers.positions(probe.getArguments());
		Set<Reified<?>> candidates = candidates(positions, args);
		return byImage.toJavaStream()
				.filter(row -> candidates == null || candidates.contains(row._1))
				.filter(row -> matches(args, Answers.positions(row._1)))
				.map(row -> Answer.of(relation, row._1, row._2))
				.collect(Collectors.toList());
	}

	long estimate(Set<Integer> positions, Call<?> probe) {
		Set<Reified<?>> candidates =
				candidates(positions, Answers.positions(probe.getArguments()));
		return candidates == null ? byImage.size() : candidates.size();
	}

	/**
	 * Ground indexed positions intersect their buckets; null means "all
	 * rows". The scratch is MUTABLE java — the stored index stays
	 * persistent, the per-probe computation never does.
	 */
	private Set<Reified<?>> candidates(Set<Integer> positions, Array<Term<Object>> args) {
		Set<Reified<?>> narrowed = null;
		for (int i = 0; i < args.size(); i++) {
			if (!positions.contains(i) || !isGround(args.get(i))) {
				continue;
			}
			Object value = args.get(i).get();
			ColumnIndex column = byColumn.getOrElse(i, null);
			Set<Reified<?>> bucket = column == null ? new HashSet<>() : column.matching(value);
			if (narrowed == null) {
				narrowed = bucket;
			} else {
				narrowed.retainAll(bucket);
			}
		}
		return narrowed;
	}

	/** Every bound probe position: the row's cell equals it, or the cell is free. */
	private static boolean matches(Array<Term<Object>> probe, Array<Term<Object>> cells) {
		return IntStream.range(0, probe.size())
				.filter(i -> isGround(probe.get(i)))
				.noneMatch(i -> isGround(cells.get(i))
						&& !Objects.equals(probe.get(i).get(), cells.get(i).get()));
	}

	private static boolean isGround(Term<?> v) {
		return v.asVal().isDefined();
	}
}
