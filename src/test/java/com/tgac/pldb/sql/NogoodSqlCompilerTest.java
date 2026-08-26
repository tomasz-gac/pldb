package com.tgac.pldb.sql;

// ABOUTME: Direct receipts for the nogood compiler: De Morgan over the registry —
// ABOUTME: literals negate via their families, disjunctions push whole or not at all.

import static com.tgac.logic.finitedomain.FiniteDomain.dom;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.constraints.Posting;
import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.finitedomain.FiniteDomainConstraints;
import com.tgac.logic.finitedomain.domains.EnumeratedDomain;
import com.tgac.logic.nogoods.Nogood;
import com.tgac.logic.nogoods.NogoodConstraints;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.ImmutableDatabase;
import com.tgac.pldb.constraints.TableConstraints;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.collection.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

public class NogoodSqlCompilerTest {

	private final Unifiable<Long> x = lvar();
	private final Unifiable<String> y = lvar();

	private final Map<Class<?>, SqlCompiler> families = new HashMap<>();

	private NogoodSqlCompiler compiler() {
		families.put(FiniteDomainConstraints.class, new FiniteDomainSqlCompiler());
		NogoodSqlCompiler nogoods = new NogoodSqlCompiler(families);
		families.put(NogoodConstraints.class, nogoods);
		return nogoods;
	}

	private Optional<SqlPredicate> compiled(Posting exclusion) {
		Atom<?> atom = ((Posting.Activation) exclusion).getItem();
		return compiler().compile(atom, this::column);
	}

	private Optional<String> column(Term<?> term) {
		return term.equals(x) ? Optional.of("id")
				: term.equals(y) ? Optional.of("name")
				: Optional.empty();
	}

	@Test
	public void aBindingLiteralNegatesDirectly() {
		Optional<SqlPredicate> p = compiled(exclude(x.unifies(3L)));
		assertThat(p).isPresent();
		assertThat(p.get().getFragment()).isEqualTo("id <> ?");
		assertThat(p.get().getParameters().toJavaList()).containsExactly(3L);
		assertThat(p.get().isExact()).isTrue();
	}

	@Test
	public void aStoreLiteralNegatesThroughItsFamily() {
		Optional<SqlPredicate> p = compiled(exclude(dom(x, EnumeratedDomain.range(1L, 3L))));
		assertThat(p).isPresent();
		assertThat(p.get().getFragment()).isEqualTo("NOT (id IN (?, ?))");
		assertThat(p.get().getParameters().toJavaList()).containsExactly(1L, 2L);
	}

	@Test
	public void aMultiLiteralConjunctDisjoinsItsNegations() {
		Optional<SqlPredicate> p = compiled(exclude(Posting.all(
				x.unifies(2L),
				y.unifies("Alan"))));
		assertThat(p).isPresent();
		assertThat(p.get().getFragment()).isEqualTo("(id <> ? OR name <> ?)");
		assertThat(p.get().getParameters().toJavaList()).containsExactly(2L, "Alan");
	}

	@Test
	public void oneUncompilableLiteralRefusesTheWholeConjunct() {
		// a literal of a family with no compiler (the posted table): pushing
		// the REST of the disjunction would strengthen — whole or not at all
		Property<Long> id = Property.of("id");
		Relations._1<Long> r = Relations.relation("r", id);
		Posting posted = TableConstraints.posted(ImmutableDatabase.empty(), r, Array.of(x));
		assertThat(compiled(exclude(Posting.all(x.unifies(2L), posted)))).isEmpty();
	}

	@Test
	public void aFusedAtomConjoinsItsConjunctsAndMayDropSome() {
		// two same-surface exclusions fuse into one atom: the conjunction may
		// push a SUBSET (dropping a conjunct weakens — the lawful direction),
		// marked weakened when it does
		Nogood first = (Nogood) ((Posting.Activation) exclude(x.unifies(2L))).getItem();
		Nogood second = (Nogood) ((Posting.Activation) exclude(x.unifies(4L))).getItem();
		Optional<SqlPredicate> both = compiler().compile(first.combine(second), this::column);
		assertThat(both).isPresent();
		assertThat(both.get().getFragment()).isEqualTo("(id <> ? AND id <> ?)");
		assertThat(both.get().isExact()).isTrue();

		Property<Long> id = Property.of("id");
		Relations._1<Long> r = Relations.relation("r", id);
		Posting posted = TableConstraints.posted(ImmutableDatabase.empty(), r, Array.of(x));
		Nogood refused = (Nogood) ((Posting.Activation) exclude(posted)).getItem();
		Optional<SqlPredicate> partial = compiler().compile(first.combine(refused), this::column);
		assertThat(partial).isPresent();
		assertThat(partial.get().getFragment()).isEqualTo("id <> ?");
		assertThat(partial.get().isExact()).isFalse();
	}
}
