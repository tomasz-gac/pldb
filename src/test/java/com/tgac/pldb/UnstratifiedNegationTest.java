package com.tgac.pldb;

// ABOUTME: Unstratified negation is a cyclic wait the substrate refuses loudly,
// ABOUTME: naming the relation's channel — because posted rules share the solve's table.

import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Literal;
import java.util.stream.Collectors;
import org.junit.Test;

public class UnstratifiedNegationTest {

	/** p(x) :- x=1, NOT p(x) — negation through its own recursion. */
	private static Literal selfNeg(Unifiable<Integer> x) {
		return Literal.relation(UnstratifiedNegationTest.class, "selfNeg")
				.arg("x", x)
				.solving(x.unifies(1).and(defer(() -> exclude(selfNeg(x)))));
	}

	@Test(timeout = 8000)
	public void unstratifiedNegationMeetsTheStrandRefusal() {
		// the nested trial consumes the OPEN entry its own production holds:
		// a genuine cyclic wait, quiescent — and the drive refuses by name
		// instead of spinning (the fresh-world regress this design replaced)
		Unifiable<Integer> x = lvar();
		assertThatThrownBy(() ->
				x.unifies(1).and(selfNeg(x)).solve(x).collect(Collectors.toList()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("blocked at unsealed")
				.hasMessageContaining("selfNeg");
	}
}
