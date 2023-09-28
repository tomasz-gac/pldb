package com.tgac.pldb;
import com.tgac.functional.exceptions.Exceptions;
import com.tgac.logic.Goal;
import com.tgac.logic.Goals;
import com.tgac.logic.LList;
import com.tgac.logic.Unifiable;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relations;
import io.vavr.Predicates;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.tgac.logic.Goal.defer;
import static com.tgac.logic.Goals.distincto;
import static com.tgac.logic.LVal.lval;
import static com.tgac.logic.LVar.lvar;
import static com.tgac.pldb.relations.Relations.relation;
import static org.assertj.core.api.Assertions.assertThat;
public class DatabaseWithRelationsTest {
	private enum Gender {
		MALE, FEMALE
	}

	private static final Property<Integer> id = Property.of("id");
	private static final Property<String> name = Property.of("name");
	private static final Property<String> surname = Property.of("surname");
	private static final Property<Gender> gender = Property.of("gender");

	private static final Relations._4<Integer, String, String, Gender> person =
			relation("person", id.indexed(), name.indexed(), surname, gender);

	private static final Property<Integer> parentId = Property.of("parentId");
	private static final Property<Integer> childId = Property.of("childId");

	private static final Relations._2<Integer, Integer> parent =
			Relations.relation("parent", parentId.indexed(), childId.indexed());

	private static Database loadGeneology(Database db) {
		return db.facts(Arrays.asList(
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
				.mapError(e -> new RuntimeException(e.collect(Collectors.joining(","))))
				.toEither()
				.fold(Exceptions::throwNow, Function.identity())
				.facts(Arrays.asList(
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
				.mapError(e -> new RuntimeException(e.collect(Collectors.joining(", "))))
				.toEither()
				.fold(Exceptions::throwNow, Function.identity());
	}

	private static final Database idb = loadGeneology(ImmutableDatabase.empty());
	private static final Database mdb = loadGeneology(new MutableDatabase());
	private static final Database db = idb;

	@Test
	public void shouldFindGrandparents() {
		Unifiable<String> gpName = lvar();
		Unifiable<String> gpSurname = lvar();

		List<String> result = Goal.runStream(lval(Tuple.of(gpName, gpSurname)),
						Goals.<Integer, Integer, Integer> exist((gpId, parentId, childId) ->
								person.apply(db, childId, lval("Tomek"), lvar(), lvar())
										.and(parent.apply(db, parentId, childId))
										.and(parent.apply(db, gpId, parentId))
										.and(person.apply(db, gpId, gpName, gpSurname, lvar()))))
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
				Goal.runStream(lval(Tuple.of(spouseName, spouseSurname)),
								Goals.<Integer, Integer, Integer> exist(
										(fatherId, childId, motherId) ->
												person.apply(db, fatherId, lval("Wiesław"), lvar(), lval(Gender.MALE))
														.and(parent.apply(db, fatherId, childId),
																parent.apply(db, motherId, childId),
																person.apply(db, motherId, spouseName, spouseSurname, lval(Gender.FEMALE)))))
						.distinct()
						.map(Unifiable::get)
						.map(DatabaseWithRelationsTest::concatNameAndSurname))
				.containsExactlyInAnyOrder("Arletta Gac", "Henryka Gac");
	}

	static Goal any(Goal... goals) {
		return s -> Arrays.stream(goals)
				.map(g -> g.apply(s))
				.filter(Predicates.not(Stream::isEmpty))
				.findFirst()
				.orElseGet(Stream::empty);
	}

	static Goal ancestors(Unifiable<Integer> descendant, Unifiable<LList<Integer>> ancestors) {
		return Goals.<Integer, LList<Integer>> exist((parentId, rest) ->
				parent.apply(db, parentId, descendant)
						.and(ancestors.unify(LList.of(parentId, rest)))
						.and(any(defer(() -> ancestors(parentId, rest)),
								rest.unify(LList.empty()))));
	}

	public static BiFunction<Unifiable<Integer>,
			Unifiable<Tuple2<Unifiable<String>, Unifiable<String>>>,
			Goal> personWithIdNameAndSurname(Database db) {
		return (id, data) -> Goals.<String, String> exist((name, surname) ->
				data.unify(Tuple.of(name, surname))
						.and(person.apply(db, id, name, surname, lvar())));
	}

	@Test
	public void shouldFindAncestors() {
		Unifiable<LList<Tuple2<Unifiable<String>, Unifiable<String>>>> ancestorNames = lvar();

		List<List<String>> result = Goal.runStream(ancestorNames,
						Goals.<Integer, LList<Integer>> exist((descendantId, l) ->
								person.apply(db, descendantId, lval("Tomek"), lvar(), lvar())
										.and(ancestors(descendantId, l))
										.and(LList.map(l, ancestorNames, personWithIdNameAndSurname(db)))))
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
		return Goals.<Integer, LList<Integer>> exist((lineHead, lineTail) ->
				line.unify(LList.empty()).and(parent.apply(db, ancestor, descendant))
						.or(line.unify(LList.of(lineHead, lineTail))
								.and(parent.apply(db, ancestor, lineHead))
								.and(defer(() -> line(lineHead, lineTail, descendant)))));
	}

	@Test
	public void shouldFindLine2() {
		Unifiable<LList<Tuple2<Unifiable<String>, Unifiable<String>>>> line = lvar();
		List<List<String>> result = Goal.runStream(line, Goals.<LList<Integer>, Integer, Integer> exist(
						(l, descendantId, ancestorId) ->
								person.apply(db, ancestorId, lval("Aniela"), lvar(), lvar())
										.and(person.apply(db, descendantId, lval("Tomek"), lvar(), lvar()),
												line(ancestorId, l, descendantId),
												LList.map(l, line, personWithIdNameAndSurname(db)))
				))
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
				.and(Goals.<Integer, LList<Integer>> exist((lineHead, lineTail) ->
						line.unify(LList.empty()).and(parent.apply(db, lhs, rhs)
										.or(parent.apply(db, rhs, lhs)))
								.or(line.unify(LList.of(lineHead, lineTail))
										.and(parent.apply(db, lhs, lineHead)
												.or(parent.apply(db, lineHead, lhs)))
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
		List<List<String>> result = Goal.runStream(line, Goals.<LList<Integer>, Integer, Integer> exist(
						(l, lhsId, rhsId) ->
								lhsId.separate(rhsId)
										.and(person.apply(db, rhsId, lval("Tomek"), lvar(), lvar()))
										.and(person.apply(db, lhsId, lval("Magda"), lvar(), lvar()),
												relatives(rhsId, lhsId, l),
												Goals.<LList<Integer>> exist(res ->
														Goals.appendo(LList.of(rhsId, l),
																		LList.of(lhsId),
																		res)
																.and(LList.map(res, line, personWithIdNameAndSurname(db)))))
										.optimize().get()))
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