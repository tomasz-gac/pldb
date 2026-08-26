package com.tgac.pldb.sql;

// ABOUTME: Direct receipts for the FD compiler: each domain kind's compiled
// ABOUTME: predicate, including the Union hull — weaker than the union, lawful.

import static com.tgac.logic.finitedomain.FiniteDomain.dom;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.constraints.Posting;
import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.finitedomain.Domain;
import com.tgac.logic.finitedomain.domains.Arithmetic;
import com.tgac.logic.finitedomain.domains.Interval;
import com.tgac.logic.finitedomain.domains.Singleton;
import com.tgac.logic.unification.Unifiable;
import java.util.Optional;
import org.junit.Test;

public class FiniteDomainSqlCompilerTest {

	private static Optional<SqlPredicate> compiled(Domain<Long> domain) {
		Unifiable<Long> x = lvar();
		Atom<?> atom = ((Posting.Activation) dom(x, domain)).getItem();
		return new FiniteDomainSqlCompiler().compile(atom, term -> Optional.of("id"));
	}

	@Test
	public void aUnionCompilesToItsHull() {
		// {1,2,4,5}: the hull BETWEEN 1 AND 5 selects a SUPERSET (3 comes
		// back, filtered locally) — weaker than the atom, the lawful direction
		Domain<Long> holey = Interval.of(1L, 5L)
				.difference(Singleton.of(Arithmetic.of(3L)));
		assertThat(holey.getClass().getSimpleName()).isEqualTo("Union");
		Optional<SqlPredicate> predicate = compiled(holey);
		assertThat(predicate).isPresent();
		assertThat(predicate.get().getFragment()).isEqualTo("id BETWEEN ? AND ?");
		assertThat(predicate.get().getParameters().toJavaList())
				.containsExactly(1L, 5L);
	}
}
