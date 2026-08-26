package com.tgac.pldb.sql;

// ABOUTME: The compiler law harness: admission judged engine-true per row, selection
// ABOUTME: judged by H2 — superset always, equality when exact, complement when negated.

import static com.tgac.logic.finitedomain.FiniteDomain.dom;
import static com.tgac.logic.finitedomain.FiniteDomain.geq;
import static com.tgac.logic.finitedomain.FiniteDomain.gtr;
import static com.tgac.logic.finitedomain.FiniteDomain.leq;
import static com.tgac.logic.finitedomain.FiniteDomain.lss;
import static com.tgac.logic.finitedomain.FiniteDomain.separate;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.constraints.Posting;
import com.tgac.logic.constraints.store.Atom;
import com.tgac.logic.constraints.store.Renaming;
import com.tgac.logic.finitedomain.FiniteDomainConstraints;
import com.tgac.logic.finitedomain.domains.Arithmetic;
import com.tgac.logic.finitedomain.domains.EnumeratedDomain;
import com.tgac.logic.finitedomain.domains.Interval;
import com.tgac.logic.finitedomain.domains.Singleton;
import com.tgac.logic.lattice.Imposition;
import com.tgac.logic.nogoods.NogoodConstraints;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.Name;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * THE THREE LAWS, checked row by row against two oracles. Admission is
 * ENGINE-TRUE: a row is admitted iff imposing the posting with the row's
 * values bound solves — no per-family admission code, the trial machinery
 * is the judge. Selection is H2's: the compiled WHERE run over the same
 * rows. The laws: selection ⊇ admission always (a compiled predicate is
 * weaker or equal — under-delivery is the one sin); selection = admission
 * when the predicate claims {@code exact}; and the negated predicate's
 * selection = the admission's complement (negation is only available while
 * exact, so the complement claim is total). The negative witness proves
 * the harness can veto: a deliberately narrowing compiler fails the
 * superset law.
 */
public class SqlCompilerLawsTest {

	private static final long LO = 1L;
	private static final long HI = 10L;

	private Connection connection;

	@Before
	public void loadH2() throws SQLException {
		connection = DriverManager.getConnection("jdbc:h2:mem:");
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE numbers(id BIGINT)");
			StringBuilder values = new StringBuilder();
			for (long v = LO; v <= HI; v++) {
				values.append(v > LO ? ", (" : "(").append(v).append(")");
			}
			ddl.execute("INSERT INTO numbers VALUES " + values);
		}
	}

	@After
	public void closeH2() throws SQLException {
		connection.close();
	}

	private Map<Class<?>, SqlCompiler> equipment() {
		Map<Class<?>, SqlCompiler> families = new HashMap<>();
		families.put(FiniteDomainConstraints.class, new FiniteDomainSqlCompiler());
		families.put(NogoodConstraints.class, new NogoodSqlCompiler(families));
		return families;
	}

	@Test
	public void theCatalogueIsLawful() {
		for (Function<Unifiable<Long>, Posting> shape : catalogue()) {
			checkLaws(shape, equipment(), true);
		}
	}

	@Test
	public void seededRandomTheoriesAreLawful() {
		for (long seed = 0; seed < 40; seed++) {
			Random random = new Random(seed);
			for (Function<Unifiable<Long>, Posting> shape : randomShapes(random)) {
				checkLaws(shape, equipment(), true);
			}
		}
	}

	@Test
	public void aNarrowingCompilerIsRejected() {
		// the negative witness: claims exactness, selects one point — the
		// superset law fails the moment the atom admits anything else
		Map<Class<?>, SqlCompiler> narrowing = new HashMap<>();
		narrowing.put(FiniteDomainConstraints.class, (atom, columns) ->
				atom instanceof Imposition ?
						columns.columnOf(((Imposition<?, ?>) atom).getTarget())
								.map(column -> SqlPredicate.eq(column, LO)) :
						Optional.empty());
		assertThatThrownBy(() -> checkLaws(
				x -> dom(x, EnumeratedDomain.range(1L, 6L)), narrowing, true))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	public void aNullableColumnDemonstratesTheConventionsReason() throws SQLException {
		// SQL three-valued logic: the NULL row satisfies NEITHER id <> 5 nor
		// its complement — a comparison silently drops it. Under the non-null
		// convention this row is illegal; this receipt is the reason on file
		try (Statement ddl = connection.createStatement()) {
			ddl.execute("INSERT INTO numbers VALUES (NULL)");
		}
		Set<Long> neq = selected(SqlPredicate.neq("id", 5L));
		Set<Long> eq = selected(SqlPredicate.eq("id", 5L));
		assertThat(neq).hasSize(9);
		assertThat(eq).hasSize(1);
		assertThat(neq.size() + eq.size())
				.describedAs("the NULL row vanished from both sides of the comparison")
				.isLessThan(11);
	}

	private List<Function<Unifiable<Long>, Posting>> catalogue() {
		List<Function<Unifiable<Long>, Posting>> shapes = new ArrayList<>();
		shapes.add(x -> dom(x, EnumeratedDomain.range(2L, 6L)));
		shapes.add(x -> dom(x, Interval.of(3L, 8L)));
		shapes.add(x -> dom(x, Interval.of(1L, 9L).difference(Singleton.of(Arithmetic.of(4L)))));
		shapes.add(x -> leq(x, lval(6L)));
		shapes.add(x -> lss(x, lval(6L)));
		shapes.add(x -> geq(x, lval(6L)));
		shapes.add(x -> gtr(x, lval(6L)));
		shapes.add(x -> separate(x, lval(6L)));
		shapes.add(x -> exclude(x.unifies(6L)));
		shapes.add(x -> exclude(dom(x, EnumeratedDomain.range(2L, 5L))));
		shapes.add(x -> exclude(exclude(x.unifies(6L))));
		return shapes;
	}

	private List<Function<Unifiable<Long>, Posting>> randomShapes(Random random) {
		long a = LO + random.nextInt((int) (HI - LO));
		long b = a + 1 + random.nextInt((int) (HI - a));
		long point = LO + random.nextInt((int) HI);
		// shapes must be PURE: the admission loop re-applies them per row,
		// and a lambda re-rolling randomness judges a different posting
		// than the one compiled
		boolean strict = random.nextBoolean();
		List<Function<Unifiable<Long>, Posting>> shapes = new ArrayList<>();
		shapes.add(x -> dom(x, Interval.of(a, b)));
		shapes.add(x -> dom(x, EnumeratedDomain.range(a, b + 1)));
		shapes.add(x -> strict ? lss(x, lval(point)) : leq(x, lval(point)));
		shapes.add(x -> exclude(x.unifies(point)));
		shapes.add(x -> exclude(Posting.all(x.unifies(a), x.unifies(b))));
		return shapes;
	}

	/** The harness: engine admission vs H2 selection, all three laws. */
	private void checkLaws(Function<Unifiable<Long>, Posting> shape,
			Map<Class<?>, SqlCompiler> families, boolean expectPushed) {
		Unifiable<Long> x = lvar();
		Posting posting = shape.apply(x);

		Set<Long> admitted = new TreeSet<>();
		for (long v = LO; v <= HI; v++) {
			Unifiable<Long> row = lvar();
			long answers = shape.apply(row)
					.and(row.unifies(v))
					.solve(lvar())
					.count();
			if (answers > 0) {
				admitted.add(v);
			}
		}

		Atom<?> atom = ((Posting.Activation) posting).getItem();
		Map<Name<?>, Term<?>> positional = new LinkedHashMap<>();
		positional.put(x.asVar().get(), Any.of(0));
		Atom<?> crossed = atom.rename(Renaming.of(positional)).ground();
		SqlCompiler compiler = families.get(atom.getFactorClass());
		assertThat(compiler).describedAs("no compiler for %s", atom.getFactorClass()).isNotNull();
		Optional<SqlPredicate> compiled = compiler.compile(crossed,
				term -> term.equals(Any.of(0)) ? Optional.of("id") : Optional.empty());

		if (!compiled.isPresent()) {
			assertThat(expectPushed)
					.describedAs("expected %s to compile", posting)
					.isFalse();
			return;
		}
		Set<Long> selected = selected(compiled.get());
		assertThat(selected)
				.describedAs("SUPERSET LAW: %s selected %s, atom admits %s",
						compiled.get(), selected, admitted)
				.containsAll(admitted);
		if (compiled.get().isExact()) {
			assertThat(selected)
					.describedAs("EXACTNESS CLAIM: %s", compiled.get())
					.isEqualTo(admitted);
		}
		compiled.get().negated().ifPresent(complement -> {
			Set<Long> rest = new TreeSet<>();
			for (long v = LO; v <= HI; v++) {
				if (!admitted.contains(v)) {
					rest.add(v);
				}
			}
			assertThat(selected(complement))
					.describedAs("COMPLEMENT LAW: %s", complement)
					.isEqualTo(rest);
		});
	}

	private Set<Long> selected(SqlPredicate predicate) {
		String sql = "SELECT id FROM numbers WHERE " + predicate.getFragment();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			int index = 1;
			for (Object parameter : predicate.getParameters()) {
				statement.setObject(index++, parameter);
			}
			try (ResultSet rows = statement.executeQuery()) {
				Set<Long> values = new TreeSet<>();
				while (rows.next()) {
					values.add(rows.getLong("id"));
				}
				return values;
			}
		} catch (SQLException e) {
			throw new IllegalStateException("selection failed: " + sql, e);
		}
	}
}
