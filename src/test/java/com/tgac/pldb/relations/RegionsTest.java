package com.tgac.pldb.relations;

// ABOUTME: Pins the live region extraction: knowledge about the probe's arg vars
// ABOUTME: crosses (unrenamed), coupled atoms stay home, no-knowledge args add nothing.

import static com.tgac.logic.finitedomain.FiniteDomain.addo;
import static com.tgac.logic.finitedomain.FiniteDomain.dom;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.finitedomain.FiniteDomainConstraints;
import com.tgac.logic.finitedomain.domains.EnumeratedDomain;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import io.vavr.collection.Array;
import org.junit.Test;

public class RegionsTest {

	@Test
	public void theRegionCarriesTheArgsKnowledgeAndLeavesCoupledAtomsHome() {
		Unifiable<Long> x = lvar();
		Unifiable<Long> y = lvar();
		Unifiable<Long> z = lvar();

		Package[] captured = new Package[1];
		Goal probe = s -> {
			captured[0] = s;
			return Cont.just(s);
		};

		long answers = dom(x, EnumeratedDomain.range(1L, 5L))
				.and(dom(y, EnumeratedDomain.range(1L, 9L)))
				.and(addo(x, lval(1L), y))
				.and(probe)
				.solve(x)
				.count();
		assertThat(answers).isGreaterThan(0);

		Residues region = Regions.about(captured[0],
				Array.of((Term<?>) x, (Term<?>) z));

		// the FD knowledge ABOUT the args crosses: exactly x's domain — the
		// addo propagator couples x to y (not an arg) and stays home, and so
		// does y's domain
		Theory<?> fd = region.getTheories()
				.get(FiniteDomainConstraints.class)
				.getOrNull();
		assertThat(fd).isNotNull();
		assertThat(fd.atoms()).hasSize(1);
		// renamed to the POSITIONAL name: x sits at arg position 0
		assertThat(fd.atoms().head().watched().contains(Any.of(0))).isTrue();

		// an arg nobody knows anything about contributes nothing
		assertThat(region.getTheories().keySet()
				.forAll(family -> family.equals(FiniteDomainConstraints.class))).isTrue();
	}
}
