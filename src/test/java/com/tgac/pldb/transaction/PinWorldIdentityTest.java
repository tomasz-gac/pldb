package com.tgac.pldb.transaction;

// ABOUTME: Pin equality is world identity: two reads of one world carry equal
// ABOUTME: pins, any commit between them makes them differ — never content compare.

import static com.tgac.logic.unification.LVal.lval;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.tabling.Call;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.logic.unification.LVar;
import com.tgac.pldb.inmemory.SharedDatabase;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import java.util.Collections;
import org.junit.Test;

public class PinWorldIdentityTest {

	private static Literal person(Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation("person")
				.arg("id", id).indexed()
				.arg("name", name)
				.from(null);
	}

	private static Call<Relation> probe() {
		Relation relation = person(LVar.lvar(), LVar.lvar()).getRel();
		Term<?>[] anys = new Term<?>[relation.getArgs().length];
		for (int i = 0; i < anys.length; i++) {
			anys[i] = Any.of(i);
		}
		return Call.of(relation, (Reified<?>) lval(Array.of(anys)));
	}

	@Test
	public void pinsOfOneWorldAreEqualAndACommitDividesThem() {
		SharedDatabase shared = SharedDatabase.empty();
		Pin first = shared.open("a").read(probe()).getPin();
		Pin second = shared.open("b").read(probe()).getPin();
		assertThat(first)
				.describedAs("two snapshots of one world name the same world")
				.isEqualTo(second);

		SimulatedSerialization mover = shared.open("mover");
		assertThat(mover.commit(Footprint.of(Collections.emptyMap()),
				Collections.singletonList(person(lval(1), lval("Ada"))))).isTrue();

		Pin third = shared.open("c").read(probe()).getPin();
		assertThat(third)
				.describedAs("a commit between two pins makes them name different worlds")
				.isNotEqualTo(first);
	}
}
