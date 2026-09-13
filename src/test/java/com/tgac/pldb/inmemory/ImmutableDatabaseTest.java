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
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ImmutableDatabaseTest {

	private static Literal man(AnswerSource db, Unifiable<String> name) {
		return Literal.relation(ImmutableDatabaseTest.class, "man")
				.arg("name", name)
				.from(db);
	}

	private static Relation manRel() {
		return man(null, lvar()).getRel();
	}

	private static Literal woman(AnswerSource db, Unifiable<String> name) {
		return Literal.relation(ImmutableDatabaseTest.class, "woman")
				.arg("name", name)
				.from(db);
	}

	private static Relation womanRel() {
		return woman(null, lvar()).getRel();
	}

	private static Literal parent(AnswerSource db, Unifiable<String> name, Unifiable<String> child) {
		return Literal.relation(ImmutableDatabaseTest.class, "parent")
				.arg("name", name)
				.arg("child", child)
				.from(db);
	}

	private static Relation parentRel() {
		return parent(null, lvar(), lvar()).getRel();
	}

	private static Literal tree(AnswerSource db, Unifiable<Integer> id, Unifiable<Integer> parentId, Unifiable<String> data) {
		return Literal.relation(ImmutableDatabaseTest.class, "tree")
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
						tree(null, lval(0), lval(0), lval("1")),

						man(null, lval("Michał")),
						man(null, lval("Franciszek")),
						man(null, lval("Czesław")),
						man(null, lval("Wacław")),
						man(null, lval("Wiesław")),
						man(null, lval("MichałW")),
						man(null, lval("Ireneusz")),
						man(null, lval("Wacław")),
						man(null, lval("Tomek")),
						woman(null, lval("Honorata")),
						woman(null, lval("Helena")),
						woman(null, lval("Ewa")),
						woman(null, lval("Janina")),
						woman(null, lval("Arletta")),
						woman(null, lval("Jolanta")),
						woman(null, lval("Henia")),
						woman(null, lval("Marta")),
						woman(null, lval("Weronika")),
						woman(null, lval("Aniela")),
						parent(null, lval("Aniela"), lval("Honorata")),
						parent(null, lval("Michał"), lval("Wiesław")),
						parent(null, lval("Helena"), lval("Wiesław")),
						parent(null, lval("Wiesław"), lval("MichałW")),
						parent(null, lval("Wiesław"), lval("Kasia")),
						parent(null, lval("Henia"), lval("Kasia")),
						parent(null, lval("Henia"), lval("MichałW")),
						parent(null, lval("Wiesław"), lval("Tomek")),
						parent(null, lval("Wiesław"), lval("Magda")),
						parent(null, lval("Honorata"), lval("Arletta")),
						parent(null, lval("Franciszek"), lval("Arletta")),
						parent(null, lval("Arletta"), lval("Tomek")),
						parent(null, lval("Arletta"), lval("Magda")),
						parent(null, lval("Czesław"), lval("Ireneusz")),
						parent(null, lval("Ewa"), lval("Ireneusz")),
						parent(null, lval("Janina"), lval("Jolanta")),
						parent(null, lval("WacławM"), lval("Jolanta")),
						parent(null, lval("Ireneusz"), lval("Marta")),
						parent(null, lval("Ireneusz"), lval("Weronika->Michał")),
						parent(null, lval("Jolanta"), lval("Marta"))))
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