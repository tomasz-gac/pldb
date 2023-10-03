package com.tgac.pldb.relations;
import com.tgac.monads.continuation.Cont;
import io.vavr.collection.List;
import org.junit.Test;

import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tgac.pldb.relations.CodeGen.codeBlock;
import static com.tgac.pldb.relations.CodeGen.genericFunctionArguments;
import static com.tgac.pldb.relations.CodeGen.genericNames;
import static com.tgac.pldb.relations.CodeGen.genericsList;
import static com.tgac.pldb.relations.CodeGen.range;
import static java.lang.String.format;
public class RelationCodeGenTest {

	@Test
	public void shouldGenerateRelations() {
		int n = 25;
		String code = Cont.<List<String>> builder()
				.just(List.of(
						"",
						"",
						"import com.tgac.functional.Consumers;",
						"import com.tgac.functional.Functions;",
						"import com.tgac.functional.Numbers;",
						"import com.tgac.functional.Tuples;",
						"import com.tgac.logic.Goal;",
						"import com.tgac.logic.LVal;",
						"import com.tgac.logic.LVar;",
						"import com.tgac.logic.Unifiable;",
						"import com.tgac.pldb.Database;",
						"import com.tgac.pldb.events.DatabaseEventListener;",
						"import com.tgac.pldb.events.DatabaseChange;",
						"import io.vavr.collection.Array;",
						"import io.vavr.collection.Seq;",
						"import io.vavr.control.Try;",
						"import lombok.AccessLevel;",
						"import lombok.NoArgsConstructor;",
						"import lombok.RequiredArgsConstructor;",
						"import lombok.Value;",
						"import java.util.Optional;",
						"",
						"import java.util.stream.Collectors;",
						"",
						"import static com.tgac.functional.Tuples.tuple;",
						"import static com.tgac.logic.LVar.lvar;",
						"",
						"",
						"@NoArgsConstructor(access = AccessLevel.PRIVATE)",
						"public class Relations"))
				.<List<String>> flatMap(l -> k -> l.appendAll(codeBlock(k.apply(List.empty()))))
				.map(l -> l.appendAll(range(0, n)
						.flatMap(i -> List.of(
										format("public static %s Relations._%s%s relation(%s)",
												genericsList(genericNames("T", i)),
												i,
												genericsList(genericNames("T", i)),
												List.of("String name")
														.appendAll(
																genericNames("T", i)
																		.map(t -> "Property<" + t + ">")
																		.zipWith(genericNames("v", i),
																				(t, v) -> t + " " + v))
														.collect(Collectors.joining(", "))))
								.appendAll(codeBlock(format("return new Relations._%s%s(%s);",
										i, i == 0 ? "" : "<>", List.of("name")
												.append(format("tuple(%s)", genericNames("v", i)
														.collect(Collectors.joining(", "))))
												.collect(Collectors.joining(", "))))))))
				.map(l -> l.appendAll(range(0, n)
						.flatMap(i -> generateRelation(i, n).append(""))))
				.apply(Function.identity())
				.collect(Collectors.joining("\n"));
		System.out.println(code);
	}
	@Test
	public void shouldGenerateRelation() {
		int n = 2;
		String code = generateRelation(n, 10)
				.collect(Collectors.joining("\n"));
		System.out.println(code);
	}

	private static List<String> generateRelation(int n, int maxN) {
		return Cont.<List<String>> builder()
				.just(List.of(
						"@Value",
						"@RequiredArgsConstructor(access = AccessLevel.PRIVATE)",
						format("public static class _%s%s implements Relation",
								n, genericsList(genericNames("T", n)))))
				.<List<String>> flatMap(l -> k -> l.appendAll(codeBlock(k.apply(List.empty()))))
				.map(l -> l.append("String name;"))
				.map(l -> l.append(format("Tuples._%s%s properties;",
								n, genericsList(genericNames("T", n)
										.map(t -> "Property<" + t + ">"))))
						.append(""))
				.map(l -> l.append("@Override")
						.append("public Property<?>[] getArgs()")
						.appendAll(codeBlock(format("return new Property<?>[]{%s};",
								genericNames("properties._", n).collect(Collectors.joining(", ")))))
						.append(""))
				.map(l -> l.append(format("public Goal apply(%s)",
								List.of("Database db")
										.appendAll(genericNames("T", n)
												.map(s -> "Unifiable<" + s + ">")
												.zipWith(genericNames("v", n),
														(t, v) -> t + " " + v))
										.collect(Collectors.joining(", "))))
						.appendAll(codeBlock(format("return RelationN.relation(%s);",
								List.of("db", "this")
										.appendAll(genericNames("v", n))
										.collect(Collectors.joining(", ")))))
						.append(""))
				.map(l -> l.append(format("public Fact fact(%s)",
								genericFunctionArguments(n)
										.collect(Collectors.joining(", "))))
						.appendAll(codeBlock(format("return Fact.of(this, Array.of(%s));",
								genericNames("v", n)
										.collect(Collectors.joining(", "))))))
				.map(l -> l.append("@Override")
						.append("public String toString()")
						.appendAll(codeBlock("return name + properties;"))
						.append(""))
				.map(l -> n < maxN - 1 ? generateDerivedRelationBuilder(n, l) : l)
				.apply(Function.identity());
	}
	private static List<String> generateFunctionApply(int n, List<String> l) {
		return l.append(format("public Goal apply(Database db, Functions._%s%s f)",
						n, genericsList(genericNames("T", n)
								.map(s -> "Unifiable<" + s + ">")
								.append("Goal"))))
				.appendAll(codeBlock(
						List.of(format("Tuples._%s%s vars = Tuples.tuple(%s);",
										n, genericsList(genericNames("T", n)
												.map(s -> "Unifiable<" + s + ">")),
										range(0, n).map(i -> "LVar.lvar()").collect(Collectors.joining(", "))))
								.append(format("return vars.apply(f).and(RelationN.relation(%s));",
										List.of("db", "this")
												.appendAll(genericNames("vars._", n))
												.collect(Collectors.joining(", "))))));
	}

	//	public Goal apply(Database db, Functions._2<Unifiable<T0>, Unifiable<T1>, Goal> f){
	//		Tuples._2<Unifiable<T0>, Unifiable<T1>> vars = tuple(LVar.lvar(), LVar.lvar());
	//		return vars.apply(f).and(apply(db, vars._0, vars._1));
	//	}

	private static List<String> generateObserver(int n, List<String> l) {
		return l.append("@SuppressWarnings(\"unchecked\")")
				.append(format("public DatabaseEventListener observer(DatabaseChange kind, Consumers._%s%s l)",
						n, genericsList(genericNames("T", n))))
				.appendAll(codeBlock(List.of(
						"return e -> Optional.of(e)",
						"\t\t.filter(f -> f.getFact().getRelation().equals(this))",
						"\t\t.filter(f -> f.getKind().equals(kind))",
						format("\t\t.map(f -> %s)",
								codeBlock(List.of(format("l.accept(%s);",
												range(0, n)
														.map(i -> format("(T%s) f.getFact().getValues().get(%s)", i, i))
														.collect(Collectors.joining(", "))),
										"return observer(kind, l);")).collect(Collectors.joining("\n"))),
						"\t\t.orElseGet(() -> observer(kind, l));")));
	}

	private static List<String> generateDerivedRelationBuilder(int n, List<String> l) {
		return l.append(format(
						"public Functions._1<Database, Try<Database>> derived(Functions._%s%s goal)",
						n + 1, genericsList(List.of("Database")
								.appendAll(genericNames("T", n)
										.map(v -> "Unifiable<" + v + ">"))
								.append("Goal"))))
				.appendAll(codeBlock(List.of(
								format("Unifiable<Tuples._%s%s> results = LVal.lval(tuple(%s));",
										n, genericsList(genericNames("T", n)
												.map(v -> "Unifiable<" + v + ">")),
										range(0, n).map(i -> "lvar()").collect(Collectors.joining(", "))))
						.append(format("return db -> db.facts(%s);",
								List.of("Goal.runStream(results, results.get().apply(goal.partial(db)))",
												".map(Unifiable::get)")
										.appendAll(range(0, n).map(i -> format(".map(t -> t.map(Numbers._%s(), Unifiable::get))", i)))
										.append(".map(t -> t.apply(this::fact))")
										.append(".collect(Collectors.toList())")
										.collect(Collectors.joining("\n"))))));
	}
}
