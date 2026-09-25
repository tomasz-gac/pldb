package org.clauseway.pldb.sql.compiler;

// ABOUTME: Direct receipts for the FD compiler: each domain kind's compiled
// ABOUTME: predicate, including the Union hull — weaker than the union, lawful.

import static org.clauseway.logic.finitedomain.FiniteDomain.dom;
import static org.clauseway.logic.unification.terms.LVal.lval;
import static org.clauseway.logic.unification.terms.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import org.clauseway.logic.constraints.Posting;
import org.clauseway.logic.constraints.store.Atom;
import org.clauseway.logic.finitedomain.Domain;
import org.clauseway.logic.finitedomain.Longs;
import org.clauseway.logic.unification.terms.Unifiable;
import java.util.Optional;
import org.junit.Test;

public class FiniteDomainSqlCompilerTest {

	private static Optional<SqlPredicate> compiled(Domain<Long> domain) {
		Unifiable<Long> x = lvar();
		Atom<?> atom = ((Posting.Activation) dom(x, domain)).getItem();
		return new FiniteDomainSqlCompiler().compile(atom, term -> Optional.of("id"));
	}

	@Test
	public void strictOrderCompilesSharp() {
		Unifiable<Long> x = lvar();
		Unifiable<Long> y = lvar();

		Atom<?> colVal = ((Posting.Activation) Longs.lss(x, lval(2L))).getItem();
		Optional<SqlPredicate> lss = new FiniteDomainSqlCompiler()
				.compile(colVal, term -> term.asVar().isPresent() ? Optional.of("id") : Optional.empty());
		assertThat(lss).isPresent();
		assertThat(lss.get().getFragment()).isEqualTo("id < ?");

		Atom<?> valCol = ((Posting.Activation) Longs.gtr(x, lval(2L))).getItem();
		Optional<SqlPredicate> gtr = new FiniteDomainSqlCompiler()
				.compile(valCol, term -> term.asVar().isPresent() ? Optional.of("id") : Optional.empty());
		assertThat(gtr).isPresent();
		assertThat(gtr.get().getFragment()).isEqualTo("id > ?");

		Atom<?> colCol = ((Posting.Activation) Longs.lss(x, y)).getItem();
		Optional<SqlPredicate> columns = new FiniteDomainSqlCompiler()
				.compile(colCol, term -> term.equals(x) ? Optional.of("lo")
						: term.equals(y) ? Optional.of("hi") : Optional.<String> empty());
		assertThat(columns).isPresent();
		assertThat(columns.get().getFragment()).isEqualTo("lo < hi");
	}

	@Test
	public void aTwiceHoledDomainCompilesFlat() {
		// two differences: {1,2} u {4,5} u {7..10} — however the domain
		// algebra nests its unions, the disjunction comes out FLAT
		Domain<Long> twiceHoley = Longs.interval(1, 10)
				.difference(Longs.singleton(3))
				.difference(Longs.singleton(6));
		Optional<SqlPredicate> predicate = compiled(twiceHoley);
		assertThat(predicate).isPresent();
		assertThat(predicate.get().getFragment())
				.isEqualTo("(id BETWEEN ? AND ? OR id BETWEEN ? AND ? OR id BETWEEN ? AND ?)");
		assertThat(predicate.get().getParameters().toJavaList())
				.containsExactly(1L, 2L, 4L, 5L, 7L, 10L);
	}

	@Test
	public void aUnionCompilesExactlyAsItsMembersDisjoined() {
		// {1,2} ∪ {4,5}: the members disjoin — the holes stay out, the
		// predicate is EXACT, and (unlike the hull it replaces) negatable
		Domain<Long> holey = Longs.interval(1, 5)
				.difference(Longs.singleton(3));
		assertThat(holey.getClass().getSimpleName()).isEqualTo("Union");
		Optional<SqlPredicate> predicate = compiled(holey);
		assertThat(predicate).isPresent();
		assertThat(predicate.get().getFragment())
				.isEqualTo("(id BETWEEN ? AND ? OR id BETWEEN ? AND ?)");
		assertThat(predicate.get().getParameters().toJavaList())
				.containsExactly(1L, 2L, 4L, 5L);
		assertThat(predicate.get().isExact()).isTrue();
	}
}
