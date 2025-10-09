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
		int n = 10;
		String code = Cont.<List<String>> builder()
				.just(List.of(
						"",
						"",
						"import com.tgac.functional.Consumers;",
						"import com.tgac.functional.Functions;",
						"import com.tgac.functional.Numbers;",
						"import com.tgac.functional.Tuples;",
						"import com.tgac.logic.Goal;",
						"import com.tgac.logic.unification.LVal;",
						"import com.tgac.logic.unification.LVar;",
						"import com.tgac.logic.unification.Unifiable;",
						"import com.tgac.pldb.Database;",
						"import com.tgac.pldb.events.FactsChanged;",
						"import com.tgac.pldb.Observer;",
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
						"import static com.tgac.logic.unification.LVar.lvar;",
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
				.map(l -> l.append(format("public Goal exists(%s)",
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
				.map(l -> generateApply(n, l))
				.apply(Function.identity());
	}
	private static List<String> generateApply(int n, List<String> l) {
		return l.append("@SuppressWarnings(\"unchecked\")")
				.append(format("public <R> Optional<R> exists(Fact fact, Functions._%s%s fn)",
						n, genericsList(genericNames("T", n).append("R"))))
				.appendAll(codeBlock(List.of(
						"return Optional.of(fact)",
						"\t\t.filter(f -> f.getRelation().equals(this))",
						format("\t\t.map(f -> fn.apply(%s));",
								CodeGen.range(0, n)
										.map(i -> "(T" + i + ")f.getValues().get(" + i + ")")
										.collect(Collectors.joining(", "))))));
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
}
