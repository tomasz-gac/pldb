package com.tgac.pldb.inmemory;

// ABOUTME: The Call-native membership store: Answer rows keyed by reified image,
// ABOUTME: conditions ⊕-fold; completeness is coverage's claim, worlds are pins'.

import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * The in-memory store, native to the seam's vocabulary. Its semantics
 * is MEMBERSHIP, and only membership: a row is the claim "this image
 * belongs to this relation, under this condition" — monotone and
 * direction-free, so answers join their relation's rows wherever the
 * probe that fetched them was narrow or wide, a wide row is the
 * ∀-schema claim over its frees, and a duplicate image ⊕-folds its
 * conditions (membership holds if EITHER derivation's guard does;
 * image equality is alpha-equivalence). The two claims the store
 * deliberately cannot speak live beside it: "these are ALL the rows
 * matching a probe" is COVERAGE's, and "this extension, as of this
 * world" is the PIN's — writes here never assert either.
 *
 * <p>Indexed columns keep per-value buckets in Term vocabulary (null
 * is a key; a row free at an indexed column lives in the wildcard set,
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
			HashSet<Reified<?>> candidates = candidates(probe.getRelation(), args);
			ArrayList<Answer> matched = new ArrayList<>();
			for (Tuple2<Reified<?>, Condition> row : byImage) {
				if (candidates != null && !candidates.contains(row._1)) {
					continue;
				}
				if (matches(args, Answers.positions(row._1))) {
					matched.add(row.apply(Answer::of));
				}
			}
			return matched;
		}

		long estimate(Call<Relation> probe) {
			HashSet<Reified<?>> candidates =
					candidates(probe.getRelation(), Answers.positions(probe.getArguments()));
			return candidates == null ? byImage.size() : candidates.size();
		}

		/**
		 * Ground indexed positions intersect their buckets; null means "all
		 * rows". The scratch is MUTABLE java — the stored index stays
		 * persistent, the per-probe computation never does.
		 */
		private HashSet<Reified<?>> candidates(Relation relation, Array<Term<Object>> args) {
			HashSet<Reified<?>> narrowed = null;
			for (int i = 0; i < args.size(); i++) {
				if (!relation.getArgs()[i].isIndexed() || !args.get(i).asVal().isDefined()) {
					continue;
				}
				Object value = args.get(i).get();
				ColumnIndex column = byColumn.getOrElse(i, null);
				HashSet<Reified<?>> bucket = column == null ? new HashSet<>() : column.matching(value);
				if (narrowed == null) {
					narrowed = bucket;
				} else {
					narrowed.retainAll(bucket);
				}
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

		/** The value's bucket plus every row free at this column, as mutable scratch. */
		HashSet<Reified<?>> matching(Object value) {
			Object key = value == null ? NULL_KEY : value;
			HashSet<Reified<?>> matched = new HashSet<>();
			buckets.getOrElse(key, LinkedHashSet.empty()).forEach(matched::add);
			wide.forEach(matched::add);
			return matched;
		}
	}
}
