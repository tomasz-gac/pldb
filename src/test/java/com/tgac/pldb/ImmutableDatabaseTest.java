package com.tgac.pldb;

import static com.tgac.logic.goals.Goal.condu;
import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.LList;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ImmutableDatabaseTest {
	private static final Property<String> name = Property.of("name");
	private static final Property<String> child = Property.of("child");
	private static final Relations._1<String> man = Relations.relation("man", name);
	private static final Relations._1<String> woman = Relations.relation("woman", name);
	private static final Relations._2<String, String> parent = Relations.relation("parent", name, child);

	private static final Property<Integer> id = Property.of("id");
	private static final Property<Integer> parentId = Property.of("parentId");
	private static final Property<String> data = Property.of("data");

	private static final Relations._3<Integer, Integer, String> tree = Relations.relation("tree", id, parentId, data);

	private static Database loadGeneology(Database db) {
		return db.withFacts(Arrays.asList(
						tree.fact(0, 0, "1"),

						man.fact("Michał"),
						man.fact("Franciszek"),
						man.fact("Czesław"),
						man.fact("Wacław"),
						man.fact("Wiesław"),
						man.fact("MichałW"),
						man.fact("Ireneusz"),
						man.fact("Wacław"),
						man.fact("Tomek"),
						woman.fact("Honorata"),
						woman.fact("Helena"),
						woman.fact("Ewa"),
						woman.fact("Janina"),
						woman.fact("Arletta"),
						woman.fact("Jolanta"),
						woman.fact("Henia"),
						woman.fact("Marta"),
						woman.fact("Weronika"),
						woman.fact("Aniela"),
						parent.fact("Aniela", "Honorata"),
						parent.fact("Michał", "Wiesław"),
						parent.fact("Helena", "Wiesław"),
						parent.fact("Wiesław", "MichałW"),
						parent.fact("Wiesław", "Kasia"),
						parent.fact("Henia", "Kasia"),
						parent.fact("Henia", "MichałW"),
						parent.fact("Wiesław", "Tomek"),
						parent.fact("Wiesław", "Magda"),
						parent.fact("Honorata", "Arletta"),
						parent.fact("Franciszek", "Arletta"),
						parent.fact("Arletta", "Tomek"),
						parent.fact("Arletta", "Magda"),
						parent.fact("Czesław", "Ireneusz"),
						parent.fact("Ewa", "Ireneusz"),
						parent.fact("Janina", "Jolanta"),
						parent.fact("WacławM", "Jolanta"),
						parent.fact("Ireneusz", "Marta"),
						parent.fact("Ireneusz", "Weronika->Michał"),
						parent.fact("Jolanta", "Marta")))
				.get();
	}

	private static final Database db = loadGeneology(ImmutableDatabase.empty());

	@Test
	public void shouldFindGrandparents() {
		Unifiable<String> grandparent = lvar();
		Unifiable<String> par = lvar();
		System.out.println(db);

		Assertions.assertThat(
						parent.exists(db, par, lval("Tomek"))
								.and(parent.exists(db, grandparent, par))
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
								parent.exists(db, lval("Wiesław"), child),
								parent.exists(db, spouse, child),
								woman.exists(db, spouse))
						.solve(spouse)
						.map(u -> u.asVal().get())
						.distinct()
						.collect(Collectors.toList()))
				.containsExactlyInAnyOrder("Arletta", "Henia");
	}

	static Goal ancestors(Unifiable<String> descendant, Unifiable<LList<String>> ancestors) {
		Unifiable<String> p = lvar();
		Unifiable<LList<String>> rest = lvar();
		return parent.exists(db, p, descendant)
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

		return line.unifies(LList.empty()).and(parent.exists(db, ancestor, descendant))
				.or(parent.exists(db, ancestor, vh)
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

	private static <T> List<T> unwrap(Unifiable<LList<T>> ll) {
		return ll.get().stream()
				.map(Either::get)
				.map(Unifiable::get)
				.collect(Collectors.toList());
	}
}