package com.tgac.pldb.inmemory;

import static com.tgac.logic.goals.Goal.condu;
import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.goals.Logic.distincto;
import static com.tgac.logic.goals.Matche.llist;
import static com.tgac.logic.goals.Matche.matche;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVal.lval;
import static com.tgac.logic.unification.LVar.lvar;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Logic;
import com.tgac.logic.goals.optimizer.CascadingOptimizer;
import com.tgac.logic.unification.LList;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
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

	private static Literal person(AnswerSource db, Unifiable<Integer> id, Unifiable<String> name, Unifiable<String> surname, Unifiable<Gender> gender) {
		return Literal.relation(DatabaseWithRelationsTest.class, "person")
				.arg("id", id).indexed()
				.arg("name", name).indexed()
				.arg("surname", surname)
				.arg("gender", gender)
				.from(db);
	}

	private static Relation personRel() {
		return person(null, lvar(), lvar(), lvar(), lvar()).getRel();
	}

	private static Literal parent(AnswerSource db, Unifiable<Integer> parentId, Unifiable<Integer> childId) {
		return Literal.relation(DatabaseWithRelationsTest.class, "parent")
				.arg("parentId", parentId).indexed()
				.arg("childId", childId).indexed()
				.from(db);
	}

	private static Relation parentRel() {
		return parent(null, lvar(), lvar()).getRel();
	}

	private enum Gender {
		MALE, FEMALE
	}

	private static final Property<Integer> id = Property.of("id");
	private static final Property<String> name = Property.of("name");
	private static final Property<String> surname = Property.of("surname");
	private static final Property<Gender> gender = Property.of("gender");

	private static final Property<Integer> parentId = Property.of("parentId");
	private static final Property<Integer> childId = Property.of("childId");

	private static Database loadGeneology(Database db) {
		return db.withFacts(Arrays.asList(
						person(null, lval(1), lval("Michał"), lval("Gac"), lval(Gender.MALE)),
						person(null, lval(2), lval("Franciszek"), lval("Żyduch"), lval(Gender.MALE)),
						person(null, lval(3), lval("Czesław"), lval("Kroc"), lval(Gender.MALE)),
						person(null, lval(4), lval("Wacław"), lval("Wiercioch"), lval(Gender.MALE)),
						person(null, lval(5), lval("Wiesław"), lval("Gac"), lval(Gender.MALE)),
						person(null, lval(6), lval("Ireneusz"), lval("Kroc"), lval(Gender.MALE)),
						person(null, lval(7), lval("Michał"), lval("Gac"), lval(Gender.MALE)),
						person(null, lval(8), lval("Tomek"), lval("Gac"), lval(Gender.MALE)),
						person(null, lval(10), lval("Aniela"), lval("X"), lval(Gender.FEMALE)),
						person(null, lval(11), lval("Honorata"), lval("Żyduch"), lval(Gender.FEMALE)),
						person(null, lval(12), lval("Helena"), lval("Gac"), lval(Gender.FEMALE)),
						person(null, lval(13), lval("Ewa"), lval("Kroc"), lval(Gender.FEMALE)),
						person(null, lval(14), lval("Janina"), lval("Wiercioch"), lval(Gender.FEMALE)),
						person(null, lval(15), lval("Arletta"), lval("Gac"), lval(Gender.FEMALE)),
						person(null, lval(16), lval("Jolanta"), lval("Kroc"), lval(Gender.FEMALE)),
						person(null, lval(17), lval("Henryka"), lval("Gac"), lval(Gender.FEMALE)),
						person(null, lval(18), lval("Kasia"), lval("Gac"), lval(Gender.FEMALE)),
						person(null, lval(19), lval("Marta"), lval("Gac"), lval(Gender.FEMALE)),
						person(null, lval(20), lval("Magda"), lval("Gac"), lval(Gender.FEMALE)),
						person(null, lval(21), lval("Weronika"), lval("Kroc"), lval(Gender.FEMALE)),
						person(null, lval(22), lval("Monika"), lval("Kroc"), lval(Gender.FEMALE))))
				.get()
				.withFacts(Arrays.asList(
						parent(null, lval(10), lval(11)),
						parent(null, lval(1), lval(5)),
						parent(null, lval(12), lval(5)),
						parent(null, lval(5), lval(7)),
						parent(null, lval(5), lval(18)),
						parent(null, lval(5), lval(20)),
						parent(null, lval(17), lval(18)),
						parent(null, lval(17), lval(7)),
						parent(null, lval(5), lval(8)),
						parent(null, lval(16), lval(19)),
						parent(null, lval(11), lval(15)),
						parent(null, lval(2), lval(15)),
						parent(null, lval(15), lval(8)),
						parent(null, lval(15), lval(20)),
						parent(null, lval(3), lval(6)),
						parent(null, lval(13), lval(6)),
						parent(null, lval(14), lval(16)),
						parent(null, lval(4), lval(16)),
						parent(null, lval(6), lval(19)),
						parent(null, lval(6), lval(22)),
						parent(null, lval(22), lval(21))))
				.get();
	}

	private static final Database db = loadGeneology(ImmutableDatabase.empty());

	@Test
	public void shouldFindGrandparents() {
		Unifiable<String> gpName = lvar();
		Unifiable<String> gpSurname = lvar();

		List<String> result =
				Logic.<Integer, Integer, Integer> exist((gpId, parentId, childId) ->
								person(db, childId, lval("Tomek"), lvar(), lvar())
										.and(parent(db, parentId, childId))
										.and(parent(db, gpId, parentId))
										.and(person(db, gpId, gpName, gpSurname, lvar())))
						.solve(lval(Tuple.of(gpName, gpSurname)))
						.map(Term::get)
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
										person(db, fatherId, lval("Wiesław"), lvar(), lval(Gender.MALE))
												.and(parent(db, fatherId, childId),
														parent(db, motherId, childId),
														person(db, motherId, spouseName, spouseSurname, lval(Gender.FEMALE))))
						.solve(lval(Tuple.of(spouseName, spouseSurname)))
						.distinct()
						.map(Term::get)
						.map(DatabaseWithRelationsTest::concatNameAndSurname))
				.containsExactlyInAnyOrder("Arletta Gac", "Henryka Gac");
	}

	static Goal ancestors(Unifiable<Integer> descendant, Unifiable<LList<Integer>> ancestors) {
		return Logic.<Integer, LList<Integer>> exist((parentId, rest) ->
				parent(db, parentId, descendant)
						.and(ancestors.unifies(LList.of(parentId, rest)))
						.and(condu(defer(() -> ancestors(parentId, rest)),
								rest.unifies(LList.empty()))));
	}

	public static BiFunction<Unifiable<Integer>,
			Unifiable<Tuple2<Unifiable<String>, Unifiable<String>>>,
			Goal> personWithIdNameAndSurname(Database db) {
		return (id, data) -> Logic.<String, String> exist((name, surname) ->
				data.unifies(Tuple.of(name, surname))
						.and(person(db, id, name, surname, lvar())));
	}

	@Test
	public void shouldFindAncestors() {
		Unifiable<LList<Tuple2<Unifiable<String>, Unifiable<String>>>> ancestorNames = lvar();

		List<List<String>> result = Logic.<Integer, LList<Integer>> exist((descendantId, l) ->
						person(db, descendantId, lval("Tomek"), lvar(), lvar())
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
				llist(() -> parent(db, ancestor, descendant)),
				llist((head, tail) ->
						parent(db, ancestor, head)
								.and(defer(() -> line(head, tail, descendant)))));
	}

	@Test
	public void shouldFindLine2() {
		Unifiable<LList<Tuple2<Unifiable<String>, Unifiable<String>>>> line = lvar();
		List<List<String>> result = Logic.<LList<Integer>, Integer, Integer> exist(
						(l, descendantId, ancestorId) ->
								person(db, ancestorId, lval("Aniela"), lvar(), lvar())
										.and(person(db, descendantId, lval("Tomek"), lvar(), lvar()),
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
						llist(() -> parent(db, lhs, rhs)),
						llist((lineHead, lineTail) ->
								parent(db, lhs, lineHead)
										.or(parent(db, lineHead, lhs))
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
								exclude(lhsId.unifies(rhsId))
										.and(person(db, rhsId, lval("Tomek"), lvar(), lvar()))
										.and(person(db, lhsId, lval("Magda"), lvar(), lvar()),
												relatives(rhsId, lhsId, l),
												Logic.<LList<Integer>> exist(res ->
														Logic.appendo(LList.of(rhsId, l),
																		LList.of(lhsId),
																		res)
																.and(LList.map(res, line, personWithIdNameAndSurname(db)))))
										.accept(new CascadingOptimizer()).ground())
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
				.withConstraint(Constraint.unique(personRel(), id))
				.withFacts(Collections.singletonList(person(null, lval(1), lval("NAME"), lval("SURNAME"), lval(Gender.MALE))));

		Assertions.assertThatThrownBy(database::get)
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	public void shouldThrowOnForeignKeyViolationOnAdd() {
		Try<Database> database = DatabaseWithRelationsTest.db
				.withConstraint(Constraint.foreignKey(parentRel(), parentId, personRel(), id))
				.withFacts(Collections.singletonList(parent(null, lval(-1), lval(1))));

		Assertions.assertThatThrownBy(database::get)
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	public void shouldThrowOnForeignKeyViolationOnRemove() {
		Try<Database> database = DatabaseWithRelationsTest.db
				.withConstraint(Constraint.foreignKey(parentRel(), parentId, personRel(), id))
				.withoutFacts(Collections.singletonList(person(null, lval(1), lval("Michał"), lval("Gac"), lval(Gender.MALE))));

		Assertions.assertThatThrownBy(database::get)
				.isInstanceOf(RuntimeException.class);
	}

	private static List<String> concatNameAndSurname(Iterable<Tuple2<Unifiable<String>, Unifiable<String>>> ll) {
		return StreamSupport.stream(ll.spliterator(), false)
				.map(DatabaseWithRelationsTest::concatNameAndSurname)
				.collect(Collectors.toList());
	}

	private static String concatNameAndSurname(Tuple2<Unifiable<String>, Unifiable<String>> t) {
		return t.map(Term::get, Term::get)
				.map2(" "::concat)
				.apply(String::concat);
	}

	private static <T> List<T> unwrap(Term<LList<T>> ll) {
		return ll.get()
				.toValueStream()
				.collect(Collectors.toList());
	}
}