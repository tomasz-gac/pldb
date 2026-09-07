package com.tgac.pldb.relations;

// ABOUTME: The function-shaped relation surface: the builder mints relation and
// ABOUTME: literal together, bare = exists, exclude converts, fact() terminal,
// ABOUTME: arity unbounded past the tuple cap.

import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class LiteralTest {

	/** The function-shaped definition: one method, names once, no relation constant. */
	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name) {
		return Literal.relation("person").arg("id", id).indexed().arg("name", name).from(db);
	}

	private static final Database db = ImmutableDatabase.empty()
			.withFacts(Arrays.asList(
					person(null, lval(1), lval("Ada")).fact(),
					person(null, lval(2), lval("Alan")).fact()))
			.get();

	private static List<String> answers(Goal g, Unifiable<?> out) {
		return g.solve(out).map(Object::toString).sorted().collect(Collectors.toList());
	}

	@Test
	public void aBareLiteralEnumerates() {
		Unifiable<Integer> i = lvar();
		Unifiable<String> n = lvar();
		assertThat(answers(person(db, i, n), n)).containsExactlyInAnyOrder("{Ada}", "{Alan}");
		Unifiable<String> n2 = lvar();
		assertThat(answers(person(db, lval(2), n2), n2)).containsExactly("{Alan}");
	}

	@Test
	public void aLiteralUnderExcludeConvertsToItsPosting() {
		Unifiable<Integer> i = lvar();
		Unifiable<String> n = lvar();
		assertThat(answers(exclude(person(db, i, n))
				.and(i.unifies(1)).and(n.unifies("Ada")), i)).isEmpty();
		Unifiable<Integer> i2 = lvar();
		Unifiable<String> n2 = lvar();
		assertThat(answers(exclude(person(db, i2, n2))
				.and(i2.unifies(1)).and(n2.unifies("Kurt")), i2)).containsExactly("{1}");
	}

	@Test
	public void twoMintsOfOneDefinitionAreTheSameRelation() {
		Unifiable<Integer> i = lvar();
		Unifiable<String> n = lvar();
		assertThat(person(db, i, n).getRel()).isEqualTo(person(db, lvar(), lvar()).getRel());
	}

	@Test
	public void factRefusesUnboundArgsByName() {
		assertThatThrownBy(() -> person(null, lval(1), lvar()).fact())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("person")
				.hasMessageContaining("name");
	}

	@Test
	public void arityOutrunsTheTupleCap() {
		// twelve columns: past vavr's 8-arity wall, through fact and query
		Unifiable<Integer> key = lvar();
		Literal wideProbe = wide(null, lval(7),
				lvar(), lvar(), lvar(), lvar(), lvar(), lvar(),
				lvar(), lvar(), lvar(), lvar(), lvar());
		Database wideDb = ImmutableDatabase.empty()
				.withFacts(Arrays.asList(
						wide(null, lval(7), lval("a"), lval("b"), lval("c"), lval("d"),
								lval("e"), lval("f"), lval("g"), lval("h"), lval("i"),
								lval("j"), lval("k")).fact()))
				.get();
		Unifiable<String> last = lvar();
		assertThat(answers(wide(wideDb, lval(7),
				lvar(), lvar(), lvar(), lvar(), lvar(), lvar(),
				lvar(), lvar(), lvar(), lvar(), last), last)).containsExactly("{k}");
	}

	private static Literal wide(AnswerSource db, Unifiable<Integer> k,
			Unifiable<String> c1, Unifiable<String> c2, Unifiable<String> c3,
			Unifiable<String> c4, Unifiable<String> c5, Unifiable<String> c6,
			Unifiable<String> c7, Unifiable<String> c8, Unifiable<String> c9,
			Unifiable<String> c10, Unifiable<String> c11) {
		return Literal.relation("wide").arg("k", k).indexed().arg("c1", c1).arg("c2", c2).arg("c3", c3).arg("c4", c4).arg("c5", c5).arg("c6", c6).arg("c7", c7).arg("c8", c8).arg("c9", c9).arg("c10", c10).arg("c11", c11).from(db);
	}

	@Test
	public void tailModifiersComposeOnTheLastColumn() {
		Literal lit = Literal.relation("flags")
				.arg("k", lvar()).indexed().ground()
				.arg("v", lvar())
				.from(db);
		Property<?>[] cols = lit.getRel().getArgs();
		assertThat(cols[0].isIndexed()).isTrue();
		assertThat(cols[0].isGround()).isTrue();
		assertThat(cols[1].isIndexed()).isFalse();
		assertThat(cols[1].isGround()).isFalse();
	}

	@Test
	public void aModifierWithoutAColumnRefuses() {
		assertThatThrownBy(() -> Literal.relation("early").indexed())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("early");
	}

	@Test
	public void aGroundColumnRefusesAnUnboundArgAtApplication() {
		Literal lit = Literal.relation("strict")
				.arg("k", lvar()).ground()
				.from(db);
		assertThatThrownBy(() -> lit.solve(lvar()).collect(Collectors.toList()))
				.hasMessageContaining("strict")
				.hasMessageContaining("k");
	}

	@Test
	public void aGroundColumnAdmitsABoundArg() {
		Unifiable<Integer> k = lvar();
		Literal lit = Literal.relation("strictOk")
				.arg("k", k).ground()
				.solving(k.unifies(5));
		assertThat(answers(k.unifies(5).and(lit), k)).containsExactly("{5}");
	}
}
