package com.tgac.pldb.relations;

// ABOUTME: The answer codec at the seam: a Fact encodes as its ground reified row
// ABOUTME: at ONE; images decode per position into values or a probe pattern.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.LVal;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The seam's value codec, written once. An answer is the cell's entry
 * shape — (reified row, {@link Condition}) — and a {@link Fact} is the
 * ground corner: its row reified whole, conditioned {@link Condition#ONE}.
 * Decoding reads POSITIONALLY through the image's structural members
 * ({@link MiniKanren#members}), never through rendering: a reified term's
 * toString decorates, the codec hands back the values.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Answers {

	/** A fact as the answer shape: its row reified ground, conditioned ONE. */
	public static Tuple2<Reified<?>, Condition> answer(Fact fact) {
		return Tuple.of((Reified<?>) lval(fact.getValues()
				.map(Object.class::cast)
				.map(LVal::lval)), Condition.ONE);
	}

	/** The row's raw values, positional. The row must be fully ground. */
	public static Array<Object> values(Reified<?> row) {
		return positions(row).map(Term::get);
	}

	/**
	 * The probe pattern the image carries: a ground position holds its
	 * value, an any is a free slot.
	 */
	public static IndexedSeq<Optional<Object>> pattern(Reified<?> image) {
		return positions(image)
				.map(position -> position.asReified().isDefined() ?
						Optional.empty() :
						Optional.of(position.get()));
	}

	@SuppressWarnings("unchecked")
	private static Array<Term<Object>> positions(Reified<?> image) {
		return Array.ofAll(MiniKanren.members(image)
						.getOrElseThrow(() -> new IllegalArgumentException(
								"not a row image: " + image)))
				.map(term -> (Term<Object>) term);
	}
}
