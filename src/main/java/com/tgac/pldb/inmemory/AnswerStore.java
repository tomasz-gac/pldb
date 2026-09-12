package com.tgac.pldb.inmemory;

// ABOUTME: The Call-native in-memory store: Answer rows keyed by reified image,
// ABOUTME: conditions ⊕-fold, per-column Term buckets serve ground probes.

import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * The in-memory store, native to the seam's vocabulary: rows are
 * {@link Answer}s — a reified image with its {@link Condition}, ground
 * rows at ONE, wide rows carrying frees — keyed by the image, so a
 * duplicate derivation ⊕-folds its condition instead of duplicating
 * the row (image equality is alpha-equivalence: the cell's law).
 * Indexed columns keep per-value buckets in Term vocabulary (null is a
 * key; a row FREE at an indexed column lives in the wildcard set,
 * matching every probe); {@link #answers} intersects the ground
 * indexed positions' buckets and filters the remaining bound
 * positions. Residues are ignored under the standing license — a
 * source may only over-deliver, and here over-delivery costs a walk.
 * Couplings between frees are likewise over-delivered; the consumer's
 * restate filters. The value is PERSISTENT: every insert mints a new
 * store, ancestors keep answering as before.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AnswerStore {

	/** Null is a legitimate bucket key; vavr maps want a witness for it. */
	private static final Object NULL_KEY = new Object();

	Map<Relation, Rows> relations;

	public static AnswerStore empty() {
		return new AnswerStore(LinkedHashMap.empty());
	}

	public AnswerStore with(Relation relation, Answer answer) {
		Rows rows = relations.getOrElse(relation, Rows.empty());
		return new AnswerStore(relations.put(relation, rows.with(relation, answer)));
	}

	public AnswerStore withAll(Relation relation, Iterable<Answer> answers) {
		AnswerStore grown = this;
		for (Answer answer : answers) {
			grown = grown.with(relation, answer);
		}
		return grown;
	}

	public Iterable<Answer> answers(Call<Relation> probe) {
		return relations.get(probe.getRelation())
				.map(rows -> rows.answers(probe))
				.getOrElse(Array.empty());
	}

	/** Upper bound from the narrowest consulted bucket — exact when unfiltered. */
	public long estimate(Call<Relation> probe) {
		return relations.get(probe.getRelation())
				.map(rows -> rows.estimate(probe))
				.getOrElse(0L);
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static class Rows {
		LinkedHashMap<Reified<?>, Condition> byImage;
		Map<Integer, ColumnIndex> byColumn;

		static Rows empty() {
			return new Rows(LinkedHashMap.empty(), HashMap.empty());
		}

		Rows with(Relation relation, Answer answer) {
			Reified<?> image = answer.getReified();
			Condition folded = byImage.get(image)
					.map(resident -> Condition.RING.plus(resident, answer.getCondition()))
					.getOrElse(answer.getCondition());
			if (byImage.containsKey(image)) {
				return new Rows(byImage.put(image, folded), byColumn);
			}
			Map<Integer, ColumnIndex> indexed = byColumn;
			Array<Term<Object>> cells = Answers.positions(image);
			for (int i = 0; i < relation.getArgs().length; i++) {
				if (!relation.getArgs()[i].isIndexed()) {
					continue;
				}
				ColumnIndex column = indexed.getOrElse(i, ColumnIndex.empty());
				indexed = indexed.put(i, column.with(cells.get(i), image));
			}
			return new Rows(byImage.put(image, folded), indexed);
		}

		Iterable<Answer> answers(Call<Relation> probe) {
			Array<Term<Object>> args = Answers.positions(probe.getArguments());
			Set<Reified<?>> candidates = candidates(probe.getRelation(), args);
			return byImage.iterator()
					.filter(row -> candidates == null || candidates.contains(row._1))
					.filter(row -> matches(args, Answers.positions(row._1)))
					.map(row -> Answer.of(row._1, row._2))
					.collect(Array.collector());
		}

		long estimate(Call<Relation> probe) {
			Set<Reified<?>> candidates =
					candidates(probe.getRelation(), Answers.positions(probe.getArguments()));
			return candidates == null ? byImage.size() : candidates.size();
		}

		/** Ground indexed positions intersect their buckets; null means "all rows". */
		private Set<Reified<?>> candidates(Relation relation, Array<Term<Object>> args) {
			Set<Reified<?>> narrowed = null;
			for (int i = 0; i < args.size(); i++) {
				if (!relation.getArgs()[i].isIndexed() || !args.get(i).asVal().isDefined()) {
					continue;
				}
				Object value = args.get(i).get();
				Set<Reified<?>> bucket = byColumn.get(i)
						.map(column -> column.matching(value))
						.getOrElse(LinkedHashSet.empty());
				narrowed = narrowed == null ? bucket : narrowed.intersect(bucket);
			}
			return narrowed;
		}

		/** Every bound probe position: the row's cell equals it, or the cell is free. */
		private static boolean matches(Array<Term<Object>> args, Array<Term<Object>> cells) {
			for (int i = 0; i < args.size(); i++) {
				if (!args.get(i).asVal().isDefined()) {
					continue;
				}
				if (cells.get(i).asVal().isDefined()
						&& !Objects.equals(cells.get(i).get(), args.get(i).get())) {
					return false;
				}
			}
			return true;
		}
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static class ColumnIndex {
		Map<Object, Set<Reified<?>>> buckets;
		Set<Reified<?>> wide;

		static ColumnIndex empty() {
			return new ColumnIndex(LinkedHashMap.empty(), LinkedHashSet.empty());
		}

		ColumnIndex with(Term<Object> cell, Reified<?> image) {
			if (!cell.asVal().isDefined()) {
				return new ColumnIndex(buckets, wide.add(image));
			}
			Object key = cell.get() == null ? NULL_KEY : cell.get();
			Set<Reified<?>> bucket = buckets.getOrElse(key, LinkedHashSet.empty());
			return new ColumnIndex(buckets.put(key, bucket.add(image)), wide);
		}

		/** The value's bucket plus every row free at this column. */
		Set<Reified<?>> matching(Object value) {
			Object key = value == null ? NULL_KEY : value;
			return buckets.getOrElse(key, LinkedHashSet.empty()).union(wide);
		}
	}
}
