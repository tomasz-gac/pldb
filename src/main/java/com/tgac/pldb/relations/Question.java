package com.tgac.pldb.relations;

// ABOUTME: The question front door: a goal plus result-row templates, answers as
// ABOUTME: Answers — rows a caller reads, a persist lands, or a retract removes.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.fibers.Scheduler;
import com.tgac.functional.fibers.schedulers.BreadthFirstScheduler;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import io.vavr.collection.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Question {

	/**
	 * The (question, schemas) shape: the question is a full logical
	 * program — any joins, negation, or domains — and each schema is a
	 * template literal minted by the relation's own function, naming the
	 * row shape and, through the lvars it shares with the question, which
	 * answer values land in which columns. Every answer grounds every
	 * template into one {@link Fact} per schema — the whole cluster from
	 * one derivation — and the facts serve every consumer alike: a caller
	 * reads them as typed rows ({@link Fact#get}), a persist stages them,
	 * a retract stages their removal. Zero answers stream nothing. A
	 * template cell the answer leaves free refuses by relation and
	 * column: facts are whole rows, always.
	 */
	public static Stream<Answer> select(Goal question, Literal... schemas) {
		return select(question, BreadthFirstScheduler::new, schemas);
	}

	public static Stream<Answer> select(Goal question, Function<Fiber<Nothing>, Scheduler<Nothing>> factory, Literal... schemas) {
		Array<Unifiable<?>> variables = variablesOf(schemas);
		return question.solve(lval(variables), factory)
				.map(Reified::get)
				.flatMap(answer -> facts(bind(variables, answer), schemas));
	}

	/** The templates' distinct variables, in birth order — the solve tuple. */
	private static Array<Unifiable<?>> variablesOf(Literal[] schemas) {
		return Arrays.stream(schemas)
				.flatMap(schema -> schema.getArgs().toJavaStream())
				.flatMap(arg -> arg.asVar()
						.map(Stream::<Unifiable<?>>of)
						.getOrElse(Stream::empty))
				.sorted(Comparator.comparing(v -> v.asVar().get().getBirth()))
				.distinct()
				.collect(Array.collector());
	}

	/**
	 * A free variable reifies to Any — a Term, never a value — so the map
	 * speaks Term and the groundness check below owns the distinction.
	 */
	private static Map<Unifiable<?>, Term<?>> bind(
			Array<Unifiable<?>> variables, Array<?> answer) {
		return IntStream.range(0, variables.size())
				.boxed()
				.collect(Collectors.toMap(variables::get, i -> (Term<?>) answer.get(i)));
	}

	/** One answer, every schema: the whole cluster this derivation names. */
	private static Stream<Answer> facts(Map<Unifiable<?>, Term<?>> bound, Literal[] schemas) {
		return Arrays.stream(schemas)
				.map(schema -> Answer.of(schema.getRel(),
						(Reified<?>) lval(IntStream.range(0, schema.getArgs().length())
								.mapToObj(i -> cell(schema, i, bound))
								.collect(Array.collector())),
						Condition.ONE));
	}

	/**
	 * The template cell under this answer: its own constant, or whatever
	 * the answer holds at the shared variable — a value, or the Any a
	 * free cell rides as.
	 */
	private static Term<?> cell(Literal schema, int position, Map<Unifiable<?>, Term<?>> bound) {
		Unifiable<?> arg = schema.getArgs().get(position);
		return arg.asVal().isDefined() ? arg : bound.get(arg);
	}
}
