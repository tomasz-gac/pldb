package com.tgac.pldb.relations;

// ABOUTME: The seam's answer shape: a reified row with its Condition — the cell's
// ABOUTME: entry as one named value, ground rows at ONE.

import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Value;

/**
 * One answer at the seam: a reified row and the {@link Condition} it
 * holds under — ground rows at {@link Condition#ONE}. This is the cell's
 * entry shape as a named value; {@link #of(Tuple2)} and {@link #tuple()}
 * cross the boundary to the tabling cell's pair vocabulary.
 */
@Value(staticConstructor = "of")
public class Answer {
	Reified<?> reified;
	Condition condition;

	public static Answer of(Tuple2<Reified<?>, Condition> entry) {
		return new Answer(entry._1, entry._2);
	}

	public Tuple2<Reified<?>, Condition> tuple() {
		return Tuple.of(reified, condition);
	}
}
