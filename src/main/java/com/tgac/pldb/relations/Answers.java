package com.tgac.pldb.relations;

// ABOUTME: The answer codec at the seam: a Fact encodes as its ground reified row
// ABOUTME: at ONE; images decode per position into cells or values.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.LVal;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import io.vavr.collection.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
	public static Answer answer(Fact fact) {
		return Answer.of((Reified<?>) lval(fact.getValues()
				.map(Object.class::cast)
				.map(LVal::lval)), Condition.ONE);
	}

	/**
	 * Whether {@code wide} SUBSUMES {@code narrow}: every instance the
	 * narrow row claims is already claimed by the wide one, under a
	 * condition at least as permissive — the licence to DROP the narrow
	 * from a delivery. Dropping is the under-delivery direction, so both
	 * checks are strict: the wide's frees must bind CONSISTENTLY (a
	 * coupled {@code (x,x)} never subsumes {@code (1,2)}), a wide ground
	 * cell must equal a ground narrow cell (a free narrow cell claims
	 * more), and the wide's condition must ABSORB the narrow's (a guarded
	 * wide never swallows an unconditional row).
	 */
	public static boolean subsumes(Answer wide, Answer narrow) {
		Array<Term<Object>> w = positions(wide.getReified());
		Array<Term<Object>> n = positions(narrow.getReified());
		if (w.size() != n.size()) {
			return false;
		}
		if (!narrow.getCondition().absorbedBy(wide.getCondition())) {
			return false;
		}
		Map<Term<Object>, Term<Object>> binding = new HashMap<>();
		for (int i = 0; i < w.size(); i++) {
			Term<Object> wc = w.get(i);
			Term<Object> nc = n.get(i);
			if (wc.asVal().isDefined()) {
				if (!nc.asVal().isDefined() || !Objects.equals(wc.get(), nc.get())) {
					return false;
				}
			} else {
				Term<Object> bound = binding.putIfAbsent(wc, nc);
				if (bound != null && !bound.equals(nc)) {
					return false;
				}
			}
		}
		return true;
	}

	/** The row's raw values, positional. The row must be fully ground. */
	public static Array<Object> values(Reified<?> row) {
		return positions(row).map(Term::get);
	}

	/**
	 * The image's cells in Term vocabulary — a ground position is a value
	 * ({@code asVal}), a free position an any ({@code asReified}) — the same
	 * vocabulary as the walked terms a row is compared against and the
	 * database index keys by, coupling identity preserved.
	 */
	@SuppressWarnings("unchecked")
	public static Array<Term<Object>> positions(Reified<?> image) {
		return Array.ofAll(MiniKanren.members(image)
						.getOrElseThrow(() -> new IllegalArgumentException(
								"not a row image: " + image)))
				.map(term -> (Term<Object>) term);
	}
}
