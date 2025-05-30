package com.tgac.pldb;

import static com.tgac.logic.goals.Goal.condu;
import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.goals.Matche.llist;
import static com.tgac.logic.goals.Matche.matche;
import static com.tgac.logic.separate.Disequality.distincto;
import static com.tgac.logic.separate.Disequality.separate;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Logic;
import com.tgac.logic.unification.LList;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DatabaseWithRelationsTest {
	private enum Gender {
		MALE, FEMALE
	}

	private static final Property<Integer> id = Property.of("id");
	private static final Property<String> name = Property.of("name");
	private static final Property<String> surname = Property.of("surname");
	private static final Property<Gender> gender = Property.of("gender");

	private static final Relations._4<Integer, String, String, Gender> person =
			Relations.relation("person", id.indexed(), name.indexed(), surname, gender);

	private static final Property<Integer> parentId = Property.of("parentId");
	private static final Property<Integer> childId = Property.of("childId");

	private static final Relations._2<Integer, Integer> parent =
			Relations.relation("parent", parentId.indexed(), childId.indexed());

	private static Database loadGeneology(Database db) {
		return db.withFacts(Arrays.asList(
						person.fact(1, "Michał", "Gac", Gender.MALE),
						person.fact(2, "Franciszek", "Żyduch", Gender.MALE),
						person.fact(3, "Czesław", "Kroc", Gender.MALE),
						person.fact(4, "Wacław", "Wiercioch", Gender.MALE),
						person.fact(5, "Wiesław", "Gac", Gender.MALE),
						person.fact(6, "Ireneusz", "Kroc", Gender.MALE),
						person.fact(7, "Michał", "Gac", Gender.MALE),
						person.fact(8, "Tomek", "Gac", Gender.MALE),
						person.fact(10, "Aniela", "X", Gender.FEMALE),
						person.fact(11, "Honorata", "Żyduch", Gender.FEMALE),
						person.fact(12, "Helena", "Gac", Gender.FEMALE),
						person.fact(13, "Ewa", "Kroc", Gender.FEMALE),
						person.fact(14, "Janina", "Wiercioch", Gender.FEMALE),
						person.fact(15, "Arletta", "Gac", Gender.FEMALE),
						person.fact(16, "Jolanta", "Kroc", Gender.FEMALE),
						person.fact(17, "Henryka", "Gac", Gender.FEMALE),
						person.fact(18, "Kasia", "Gac", Gender.FEMALE),
						person.fact(19, "Marta", "Gac", Gender.FEMALE),
						person.fact(20, "Magda", "Gac", Gender.FEMALE),
						person.fact(21, "Weronika", "Kroc", Gender.FEMALE),
						person.fact(22, "Monika", "Kroc", Gender.FEMALE)))
				.get()
				.withFacts(Arrays.asList(
						parent.fact(10, 11),
						parent.fact(1, 5),
						parent.fact(12, 5),
						parent.fact(5, 7),
						parent.fact(5, 18),
						parent.fact(5, 20),
						parent.fact(17, 18),
						parent.fact(17, 7),
						parent.fact(5, 8),
						parent.fact(16, 19),
						parent.fact(11, 15),
						parent.fact(2, 15),
						parent.fact(15, 8),
						parent.fact(15, 20),
						parent.fact(3, 6),
						parent.fact(13, 6),
						parent.fact(14, 16),
						parent.fact(4, 16),
						parent.fact(6, 19),
						parent.fact(6, 22),
						parent.fact(22, 21)))
				.get();
	}

	private static final Database db = loadGeneology(ImmutableDatabase.empty());

	@Test
	public void shouldFindGrandparents() {
		Unifiable<String> gpName = lvar();
		Unifiable<String> gpSurname = lvar();

		List<String> result =
				Logic.<Integer, Integer, Integer> exist((gpId, parentId, childId) ->
								person.exists(db, childId, lval("Tomek"), lvar(), lvar())
										.and(parent.exists(db, parentId, childId))
										.and(parent.exists(db, gpId, parentId))
										.and(person.exists(db, gpId, gpName, gpSurname, lvar())))
						.solve(lval(Tuple.of(gpName, gpSurname)))
						.map(Unifiable::get)
						.map(DatabaseWithRelationsTest::concatNameAndSurname)
						.distinct()
						.collect(Collectors.toList());
		System.out.println(result);
		assertThat(result)
				.containsExactlyInAnyOrder(
						"Franciszek Żyduch",
						"Michał Gac",
						"Helena Gac",
						"Honorata Żyduch");
	}

	@Test
	public void shouldFindParentSpouse() {
		Unifiable<String> spouseName = lvar();
		Unifiable<String> spouseSurname = lvar();

		assertThat(
				Logic.<Integer, Integer, Integer> exist(
								(fatherId, childId, motherId) ->
										person.exists(db, fatherId, lval("Wiesław"), lvar(), lval(Gender.MALE))
												.and(parent.exists(db, fatherId, childId),
														parent.exists(db, motherId, childId),
														person.exists(db, motherId, spouseName, spouseSurname, lval(Gender.FEMALE))))
						.solve(lval(Tuple.of(spouseName, spouseSurname)))
						.distinct()
						.map(Unifiable::get)
						.map(DatabaseWithRelationsTest::concatNameAndSurname))
				.containsExactlyInAnyOrder("Arletta Gac", "Henryka Gac");
	}

	static Goal ancestors(Unifiable<Integer> descendant, Unifiable<LList<Integer>> ancestors) {
		return Logic.<Integer, LList<Integer>> exist((parentId, rest) ->
				parent.exists(db, parentId, descendant)
						.and(ancestors.unifies(LList.of(parentId, rest)))
						.and(condu(defer(() -> ancestors(parentId, rest)),
								rest.unifies(LList.empty()))));
	}

	public static BiFunction<Unifiable<Integer>,
			Unifiable<Tuple2<Unifiable<String>, Unifiable<String>>>,
			Goal> personWithIdNameAndSurname(Database db) {
		return (id, data) -> Logic.<String, String> exist((name, surname) ->
				data.unifies(Tuple.of(name, surname))
						.and(person.exists(db, id, name, surname, lvar())));
	}

	@Test
	public void shouldFindAncestors() {
		Unifiable<LList<Tuple2<Unifiable<String>, Unifiable<String>>>> ancestorNames = lvar();

		List<List<String>> result = Logic.<Integer, LList<Integer>> exist((descendantId, l) ->
						person.exists(db, descendantId, lval("Tomek"), lvar(), lvar())
								.and(ancestors(descendantId, l))
								.and(LList.map(l, ancestorNames, personWithIdNameAndSurname(db))))
				.solve(ancestorNames)
				.map(DatabaseWithRelationsTest::unwrap)
				.map(DatabaseWithRelationsTest::concatNameAndSurname)
				.collect(Collectors.toList());

		assertThat(result)
				.containsExactlyInAnyOrder(
						Arrays.asList("Wiesław Gac", "Helena Gac"),
						Arrays.asList("Arletta Gac", "Franciszek Żyduch"),
						Arrays.asList("Wiesław Gac", "Michał Gac"),
						Arrays.asList("Arletta Gac", "Honorata Żyduch", "Aniela X"));

	}

	static Goal line(Unifiable<Integer> ancestor, Unifiable<LList<Integer>> line, Unifiable<Integer> descendant) {
		return matche(line,
				llist(() -> parent.exists(db, ancestor, descendant)),
				llist((head, tail) ->
						parent.exists(db, ancestor, head)
								.and(defer(() -> line(head, tail, descendant)))));
	}

	@Test
	public void shouldFindLine2() {
		Unifiable<LList<Tuple2<Unifiable<String>, Unifiable<String>>>> line = lvar();
		List<List<String>> result = Logic.<LList<Integer>, Integer, Integer> exist(
						(l, descendantId, ancestorId) ->
								person.exists(db, ancestorId, lval("Aniela"), lvar(), lvar())
										.and(person.exists(db, descendantId, lval("Tomek"), lvar(), lvar()),
												line(ancestorId, l, descendantId),
												LList.map(l, line, personWithIdNameAndSurname(db))))
				.solve(line)
				.map(DatabaseWithRelationsTest::unwrap)
				.map(DatabaseWithRelationsTest::concatNameAndSurname)
				.collect(Collectors.toList());
		assertThat(result)
				.containsExactly(Arrays.asList("Honorata Żyduch", "Arletta Gac"));
	}

	static Goal relativesImpl(Unifiable<Integer> lhs,
			Unifiable<Integer> rhs,
			Unifiable<LList<Integer>> line,
			Unifiable<LList<Integer>> checked) {
		return distincto(checked)
				.and(matche(line,
						llist(() -> parent.exists(db, lhs, rhs)),
						llist((lineHead, lineTail) ->
								parent.exists(db, lhs, lineHead)
										.or(parent.exists(db, lineHead, lhs))
										.and(defer(() -> relativesImpl(lineHead, rhs, lineTail,
												LList.of(rhs, LList.of(lineHead, checked)))))
										.and(distincto(line)))));
	}

	static Goal relatives(Unifiable<Integer> lhs, Unifiable<Integer> rhs, Unifiable<LList<Integer>> line) {
		return relativesImpl(rhs, lhs, line, LList.empty());
	}

	@Test
	public void shouldFindRelatives() {
		Unifiable<LList<Tuple2<Unifiable<String>, Unifiable<String>>>> line = lvar();
		List<List<String>> result = Logic.<LList<Integer>, Integer, Integer> exist(
						(l, lhsId, rhsId) ->
								separate(lhsId, rhsId)
										.and(person.exists(db, rhsId, lval("Tomek"), lvar(), lvar()))
										.and(person.exists(db, lhsId, lval("Magda"), lvar(), lvar()),
												relatives(rhsId, lhsId, l),
												Logic.<LList<Integer>> exist(res ->
														Logic.appendo(LList.of(rhsId, l),
																		LList.of(lhsId),
																		res)
																.and(LList.map(res, line, personWithIdNameAndSurname(db)))))
										.optimize().get())
				.solve(line)
				.map(DatabaseWithRelationsTest::unwrap)
				.map(DatabaseWithRelationsTest::concatNameAndSurname)
				.collect(Collectors.toList());
		System.out.println(result.stream()
				.map(l -> String.join(", ", l))
				.collect(Collectors.joining("\n")));
		assertThat(result)
				.containsExactlyInAnyOrder(
						Arrays.asList("Tomek Gac", "Arletta Gac", "Magda Gac"),
						Arrays.asList("Tomek Gac", "Wiesław Gac", "Magda Gac"));
	}

	@Test
	public void shouldThrowOnUniqueConstraintViolation() {
		Try<Database> database = DatabaseWithRelationsTest.db
				.withConstraint(Constraint.unique(person, id))
				.withFacts(Collections.singletonList(person.fact(1, "NAME", "SURNAME", Gender.MALE)));

		Assertions.assertThatThrownBy(database::get)
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	public void shouldThrowOnForeignKeyViolationOnAdd() {
		Try<Database> database = DatabaseWithRelationsTest.db
				.withConstraint(Constraint.foreignKey(parent, parentId, person, id))
				.withFacts(Collections.singletonList(parent.fact(-1, 1)));

		Assertions.assertThatThrownBy(database::get)
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	public void shouldThrowOnForeignKeyViolationOnRemove() {
		Try<Database> database = DatabaseWithRelationsTest.db
				.withConstraint(Constraint.foreignKey(parent, parentId, person, id))
				.withoutFacts(Collections.singletonList(person.fact(1, "Michał", "Gac", Gender.MALE)));

		Assertions.assertThatThrownBy(database::get)
				.isInstanceOf(RuntimeException.class);
	}

	private static List<String> concatNameAndSurname(Iterable<Tuple2<Unifiable<String>, Unifiable<String>>> ll) {
		return StreamSupport.stream(ll.spliterator(), false)
				.map(DatabaseWithRelationsTest::concatNameAndSurname)
				.collect(Collectors.toList());
	}

	private static String concatNameAndSurname(Tuple2<Unifiable<String>, Unifiable<String>> t) {
		return t.map(Unifiable::get, Unifiable::get)
				.map2(" "::concat)
				.apply(String::concat);
	}

	private static <T> List<T> unwrap(Unifiable<LList<T>> ll) {
		return ll.get()
				.toValueStream()
				.collect(Collectors.toList());
	}
}