package com.tgac.pldb;
import com.tgac.logic.Goal;
import com.tgac.logic.LList;
import com.tgac.logic.Unifiable;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.Predicates;
import io.vavr.collection.Stream;
import io.vavr.control.Either;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.tgac.logic.Goal.defer;
import static com.tgac.logic.Goals.firsto;
import static com.tgac.logic.LVal.lval;
import static com.tgac.logic.LVar.lvar;
import static com.tgac.pldb.relations.Relations.relation;
public class ImmutableDatabaseTest {
	private static final Property<String> name = Property.of("name");
	private static final Property<String> child = Property.of("child");
	private static final Relations._1<String> man = relation("man", name);
	private static final Relations._1<String> woman = relation("woman", name);
	private static final Relations._2<String, String> parent = relation("parent", name, child);

	private static Database loadGeneology(Database db) {
		return db.facts(Arrays.asList(
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

	private static final Database idb = loadGeneology(ImmutableDatabase.empty());
	private static final Database mdb = loadGeneology(new MutableDatabase());
	private static final Database db = idb;

	@Test
	public void shouldFindGrandparents() {
		Unifiable<String> grandparent = lvar();
		Unifiable<String> par = lvar();
		System.out.println(db);

		Assertions.assertThat(Goal.runStream(grandparent,
								parent.apply(db, par, lval("Tomek")),
								parent.apply(db, grandparent, par))
						.map(u -> u.asVal().get())
						.collect(Collectors.toList()))
				.containsExactlyInAnyOrder("Franciszek", "Michał", "Helena", "Honorata");
	}

	@Test
	public void shouldFindParentSpouse() {
		Unifiable<String> spouse = lvar();
		Unifiable<String> child = lvar();

		Assertions.assertThat(Goal.runStream(spouse,
								parent.apply(db, lval("Wiesław"), child),
								parent.apply(db, spouse, child),
								woman.apply(db, spouse))
						.map(u -> u.asVal().get())
						.distinct()
						.collect(Collectors.toList()))
				.containsExactlyInAnyOrder("Arletta", "Henia");
	}

	static Goal any(Goal... goals) {
		return s -> Arrays.stream(goals)
				.map(g -> g.apply(s))
				.filter(Predicates.not(Stream::isEmpty))
				.findFirst()
				.orElseGet(Stream::empty);
	}

	static Goal ancestors(Unifiable<String> descendant, Unifiable<LList<String>> ancestors) {
		Unifiable<String> p = lvar();
		Unifiable<LList<String>> rest = lvar();
		return parent.apply(db, p, descendant)
				.and(ancestors.unify(LList.of(p, rest)))
				.and(firsto(defer(() -> ancestors(p, rest)),
						rest.unify(LList.empty())));
	}

	@Test
	public void shouldFindLine() {
		Unifiable<LList<String>> l = lvar();
		List<List<String>> result = Goal.runStream(l,
						ancestors(lval("Tomek"), l))
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

		return line.unify(LList.empty()).and(parent.apply(db, ancestor, descendant))
				.or(parent.apply(db, ancestor, vh)
						.and(line.unify(LList.of(vh, vd)))
						.and(defer(() -> line(vh, vd, descendant))));
	}

	@Test
	public void shouldFindLine2() {
		Unifiable<LList<String>> l = lvar();
		List<List<String>> result = Goal.runStream(l,
						line(lval("Aniela"), l, lval("Tomek")))
				.map(ImmutableDatabaseTest::unwrap)
				.collect(Collectors.toList());
		Assertions.assertThat(result)
				.containsExactly(Arrays.asList("Honorata", "Arletta"));
	}

	@Test
	public void shouldFindDescendant() {
		Unifiable<LList<String>> l = lvar();
		List<List<String>> result = Goal.runStream(l,
						ancestors(lval("Tomek"), l))
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