package com.tgac.pldb.inmemory;

import static com.tgac.logic.goals.Goal.condu;
import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.LList;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.RelationN;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ImmutableDatabaseTest {

	private static Literal man(AnswerSource db, Unifiable<String> name) {
		return Literal.relation("man")
				.arg("name", name)
				.from(db);
	}

	private static Relation manRel() {
		return man(null, lvar()).getRel();
	}

	private static Literal woman(AnswerSource db, Unifiable<String> name) {
		return Literal.relation("woman")
				.arg("name", name)
				.from(db);
	}

	private static Relation womanRel() {
		return woman(null, lvar()).getRel();
	}

	private static Literal parent(AnswerSource db, Unifiable<String> name, Unifiable<String> child) {
		return Literal.relation("parent")
				.arg("name", name)
				.arg("child", child)
				.from(db);
	}

	private static Relation parentRel() {
		return parent(null, lvar(), lvar()).getRel();
	}

	private static Literal tree(AnswerSource db, Unifiable<Integer> id, Unifiable<Integer> parentId, Unifiable<String> data) {
		return Literal.relation("tree")
				.arg("id", id)
				.arg("parentId", parentId)
				.arg("data", data)
				.from(db);
	}

	private static Relation treeRel() {
		return tree(null, lvar(), lvar(), lvar()).getRel();
	}
	private static final Property<String> name = Property.of("name");
	private static final Property<String> child = Property.of("child");




	private static final Property<Integer> id = Property.of("id");
	private static final Property<Integer> parentId = Property.of("parentId");
	private static final Property<String> data = Property.of("data");



	private static Database loadGeneology(Database db) {
		return db.withFacts(Arrays.asList(
						tree(null, lval(0), lval(0), lval("1")).fact(),

						man(null, lval("Michał")).fact(),
						man(null, lval("Franciszek")).fact(),
						man(null, lval("Czesław")).fact(),
						man(null, lval("Wacław")).fact(),
						man(null, lval("Wiesław")).fact(),
						man(null, lval("MichałW")).fact(),
						man(null, lval("Ireneusz")).fact(),
						man(null, lval("Wacław")).fact(),
						man(null, lval("Tomek")).fact(),
						woman(null, lval("Honorata")).fact(),
						woman(null, lval("Helena")).fact(),
						woman(null, lval("Ewa")).fact(),
						woman(null, lval("Janina")).fact(),
						woman(null, lval("Arletta")).fact(),
						woman(null, lval("Jolanta")).fact(),
						woman(null, lval("Henia")).fact(),
						woman(null, lval("Marta")).fact(),
						woman(null, lval("Weronika")).fact(),
						woman(null, lval("Aniela")).fact(),
						parent(null, lval("Aniela"), lval("Honorata")).fact(),
						parent(null, lval("Michał"), lval("Wiesław")).fact(),
						parent(null, lval("Helena"), lval("Wiesław")).fact(),
						parent(null, lval("Wiesław"), lval("MichałW")).fact(),
						parent(null, lval("Wiesław"), lval("Kasia")).fact(),
						parent(null, lval("Henia"), lval("Kasia")).fact(),
						parent(null, lval("Henia"), lval("MichałW")).fact(),
						parent(null, lval("Wiesław"), lval("Tomek")).fact(),
						parent(null, lval("Wiesław"), lval("Magda")).fact(),
						parent(null, lval("Honorata"), lval("Arletta")).fact(),
						parent(null, lval("Franciszek"), lval("Arletta")).fact(),
						parent(null, lval("Arletta"), lval("Tomek")).fact(),
						parent(null, lval("Arletta"), lval("Magda")).fact(),
						parent(null, lval("Czesław"), lval("Ireneusz")).fact(),
						parent(null, lval("Ewa"), lval("Ireneusz")).fact(),
						parent(null, lval("Janina"), lval("Jolanta")).fact(),
						parent(null, lval("WacławM"), lval("Jolanta")).fact(),
						parent(null, lval("Ireneusz"), lval("Marta")).fact(),
						parent(null, lval("Ireneusz"), lval("Weronika->Michał")).fact(),
						parent(null, lval("Jolanta"), lval("Marta")).fact()))
				.get();
	}

	private static final Database db = loadGeneology(ImmutableDatabase.empty());

	@Test
	public void shouldFindGrandparents() {
		Unifiable<String> grandparent = lvar();
		Unifiable<String> par = lvar();
		System.out.println(db);

		Assertions.assertThat(
						parent(db, par, lval("Tomek"))
								.and(parent(db, grandparent, par))
								.solve(grandparent)
								.map(u -> u.asVal().get())
								.collect(Collectors.toList()))
				.containsExactlyInAnyOrder("Franciszek", "Michał", "Helena", "Honorata");
	}

	@Test
	public void shouldFindParentSpouse() {
		Unifiable<String> spouse = lvar();
		Unifiable<String> child = lvar();

		Assertions.assertThat(Goal.success().and(
								parent(db, lval("Wiesław"), child),
								parent(db, spouse, child),
								woman(db, spouse))
						.solve(spouse)
						.map(u -> u.asVal().get())
						.distinct()
						.collect(Collectors.toList()))
				.containsExactlyInAnyOrder("Arletta", "Henia");
	}

	static Goal ancestors(Unifiable<String> descendant, Unifiable<LList<String>> ancestors) {
		Unifiable<String> p = lvar();
		Unifiable<LList<String>> rest = lvar();
		return parent(db, p, descendant)
				.and(ancestors.unifies(LList.of(p, rest)))
				.and(condu(defer(() -> ancestors(p, rest)),
						rest.unifies(LList.empty())));
	}

	@Test
	public void shouldFindLine() {
		Unifiable<LList<String>> l = lvar();
		List<List<String>> result =
				ancestors(lval("Tomek"), l)
						.solve(l)
						.map(ImmutableDatabaseTest::unwrap)
						.collect(Collectors.toList());

		Assertions.assertThat(result)
				.containsExactlyInAnyOrder(
						Arrays.asList("Wiesław", "Helena"),
						Arrays.asList("Arletta", "Franciszek"),
						Arrays.asList("Wiesław", "Michał"),
						Arrays.asList("Arletta", "Honorata", "Aniela"));

	}

	static Goal line(Unifiable<String> ancestor, Unifiable<LList<String>> line, Unifiable<String> descendant) {
		Unifiable<String> vh = lvar();
		Unifiable<LList<String>> vd = lvar();

		return line.unifies(LList.empty()).and(parent(db, ancestor, descendant))
				.or(parent(db, ancestor, vh)
						.and(line.unifies(LList.of(vh, vd)))
						.and(defer(() -> line(vh, vd, descendant))));
	}

	@Test
	public void shouldFindLine2() {
		Unifiable<LList<String>> l = lvar();
		List<List<String>> result =
				line(lval("Aniela"), l, lval("Tomek"))
						.solve(l)
						.map(ImmutableDatabaseTest::unwrap)
						.collect(Collectors.toList());
		Assertions.assertThat(result)
				.containsExactly(Arrays.asList("Honorata", "Arletta"));
	}

	@Test
	public void shouldFindDescendant() {
		Unifiable<LList<String>> l = lvar();
		List<List<String>> result =
				ancestors(lval("Tomek"), l)
						.solve(l)
						.map(ImmutableDatabaseTest::unwrap)
						.collect(Collectors.toList());
		Assertions.assertThat(result)
				.containsExactlyInAnyOrder(
						Arrays.asList("Wiesław", "Helena"),
						Arrays.asList("Arletta", "Franciszek"),
						Arrays.asList("Wiesław", "Michał"),
						Arrays.asList("Arletta", "Honorata", "Aniela"));
	}

	private static <T> List<T> unwrap(Term<LList<T>> ll) {
		return ll.get().stream()
				.map(Either::get)
				.map(Term::get)
				.collect(Collectors.toList());
	}
}