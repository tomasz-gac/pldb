package com.tgac.pldb.relations;

import com.tgac.monads.functional.Functions;
import io.vavr.Predicates;
import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CodeGen {

	public static List<String> genericFunctionArguments(int n) {
		return genericNames("T", n)
				.zipWith(genericNames("v", n),
						(t, v) -> t + " " + v);
	}

	public static List<String> block(char begin, char end, List<String> contents) {
		return List.of(String.valueOf(begin))
				.appendAll(contents)
				.append(String.valueOf(end));
	}

	public static List<String> blockIfContents(char begin, char end, List<String> contents) {
		return contents
				.filter(Predicates.not(String::isEmpty))
				.isEmpty() ?
				List.empty() :
				block(begin, end, contents);
	}

	public static List<String> codeBlock(List<String> contents) {
		return block('{', '}', contents.map("\t"::concat));
	}

	public static List<String> argListNewline(List<String> contents) {
		return block('(', ')', contents.map("\t"::concat));
	}

	public static List<String> codeBlock(String contents) {
		return codeBlock(List.of(contents));
	}

	public static String genericsList(List<String> contents) {
		return blockIfContents('<', '>',
				List.of(contents.collect(Collectors.joining(", "))))
				.collect(Collectors.joining());
	}

	public static List<String> genericNames(String name, int maxIndex) {
		return range(0, maxIndex)
				.map(Functions.function(CodeGen::genericName)
						.partial(name)::apply);
	}

	public static List<Integer> range(int from, int toExcluding) {
		return List.range(from, toExcluding);
	}

	public static String genericName(String name, int n) {
		return name + n;
	}

}
