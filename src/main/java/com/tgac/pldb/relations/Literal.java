package com.tgac.pldb.relations;

// ABOUTME: A relation lookup as a data goal: planner-visible, priced by its
// ABOUTME: index bucket — the Bounded citizen that collapses the pldb planner to data.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.fibers.Fiber;
import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.Postable;
import com.tgac.logic.constraints.Posting;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.goals.optimizer.Bounded;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.tabling.Tabling;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Reified;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Unifiable;
import com.tgac.logic.tabling.Table;
import com.tgac.pldb.AnswerProducer;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.GoalProducer;
import com.tgac.pldb.constraints.TableConstraints;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * The lookup captures its own backend — one of the seam's two kinds — so
 * cost estimates need no context: {@link #answers} counts the index
 * bucket the probe would hit under the current bindings — the order
 * function of the narrowing/widening taxonomy (logic's
 * docs/design/optimizer.md §3-4). The probe IS the call key, minted at
 * the one reification site ({@link Residues#about}). Delivery is uniform
 * across kinds and conditions: each answer forks per condition conjunct,
 * and one conjunct's delivery is {@link Residues#restate} at the query
 * anchor — the image half unifies the row, the factor half imposes the
 * constraints; a ground row is the corner where the condition is ONE and
 * restate is pure unification. The SYNC kind enumerates inline; the
 * ASYNC kind streams — emissions deliver as the cell grows, and the seal
 * ends the branch.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Literal implements Goal, Bounded, Postable {
	Either<AnswerSource, AnswerProducer> backend;
	Relation rel;
	Array<Unifiable<?>> args;
	Goal rule;

	public static Literal of(AnswerSource source, Relation rel, Array<Unifiable<?>> args) {
		return new Literal(Either.left(source), rel, args, null);
	}

	public static Literal of(AnswerProducer producer, Relation rel, Array<Unifiable<?>> args) {
		return new Literal(Either.right(producer), rel, args, null);
	}

	/**
	 * The rule-backed kind: a derived relation as an ordinary tabled goal in
	 * the SOLVE's table, keyed by the relation value — every mint of one
	 * definition names the same entries, recursion is the method calling
	 * itself (an ordinary consumer completion detection seals), and the body
	 * rides this mint (the claiming call's body runs; one definition per
	 * name per solve is the discipline).
	 */
	public static Literal solving(String name, Goal body) {
		return new Literal(null, RelationN.of(name), Array.empty(), body);
	}

	/**
	 * The function-shaped front door: relation and literal minted together,
	 * one column per {@link #arg}/{@link #indexed}/{@link #ground} call —
	 * names stated once, arity unbounded, the defining method's signature
	 * the only typed surface. Relation identity is by value (name plus
	 * columns), so every mint of one definition is the same relation.
	 */
	public static Literal of(String name, AnswerSource source) {
		return new Literal(Either.left(source), RelationN.of(name), Array.empty(), null);
	}

	public static Literal of(String name, AnswerProducer producer) {
		return new Literal(Either.right(producer), RelationN.of(name), Array.empty(), null);
	}

	public Literal arg(String column, Unifiable<?> value) {
		return extended(Property.of(column), value);
	}

	public Literal indexed(String column, Unifiable<?> value) {
		return extended(Property.of(column).indexed(), value);
	}

	public Literal ground(String column, Unifiable<?> value) {
		return extended(Property.of(column).ground(), value);
	}

	private Literal extended(Property<?> column, Unifiable<?> value) {
		Property<?>[] existing = rel.getArgs();
		Property<?>[] wider = Arrays.copyOf(existing, existing.length + 1);
		wider[existing.length] = column;
		return new Literal(backend, RelationN.of(rel.getName(), wider), args.append(value), rule);
	}

	/** The imposition reading — under {@code exclude} this is the only one. */
	@Override
	public Posting posted() {
		if (rule != null) {
			return TableConstraints.posted(GoalProducer.of(rel, rule, args, Table.empty()), rel, args);
		}
		return backend.fold(
				source -> TableConstraints.posted(source, rel, args),
				producer -> TableConstraints.posted(producer, rel, args));
	}

	/**
	 * The literal as a stored row: every column must be a ground value.
	 * Refuses loudly by relation and column name — a fact with a hole is a
	 * question, not knowledge.
	 */
	public Fact fact() {
		Array<Object> values = args.zipWithIndex().map(t -> {
			if (!t._1.asVal().isDefined()) {
				throw new IllegalStateException("fact() over " + rel.getName()
						+ ": column '" + rel.getArgs()[t._2].getName() + "' is unbound");
			}
			return (Object) t._1.asVal().get();
		});
		return Fact.of(rel, values);
	}

	@Override
	public Cont<Package, Nothing> apply(Package s) {
		if (rule != null) {
			return Tabling.call(rel, args.map(Unifiable::getObjectUnifiable), () -> rule).apply(s);
		}
		return Cont.defer(() -> substituteQueryItems(s.substitution(), args)
				.flatMap(q -> {
					Unifiable<?> anchor = lval(q.map(Unifiable::getObjectUnifiable));
					return Residues.about(s, anchor)
							.map(key -> dispatch(Call.of(rel, key._1, key._2), anchor).apply(s));
				}));
	}

	/** The sync kind enumerates inline; the async kind streams through produce. */
	private Goal dispatch(Call<Relation> probe, Unifiable<?> anchor) {
		return backend.fold(
				source -> sync(probe, anchor, source),
				producer -> st -> k -> producer.produce(probe,
						answer -> deliver(answer, anchor).apply(st).apply(k)));
	}

	private Goal sync(Call<Relation> probe, Unifiable<?> anchor, AnswerSource source) {
		return StreamSupport.stream(source.answers(probe).spliterator(), false)
				.flatMap(answer -> answer._2.conjuncts().toJavaStream()
						.map(conjunct -> (Goal) Residues.restate(answer._1, conjunct, anchor)))
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
	}

	/** One answer, forked per condition conjunct, each restated at the anchor. */
	private static Goal deliver(Tuple2<Reified<?>, Condition> answer, Unifiable<?> anchor) {
		return answer._2.conjuncts().toJavaStream()
				.map(conjunct -> (Goal) Residues.restate(answer._1, conjunct, anchor))
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
	}

	@Override
	public long answers(Substitutions s) {
		Reified<?> image = MiniKanren.reify(s,
						lval(args.map(u -> (Unifiable<?>) s.walk(u))
								.map(Unifiable::getObjectUnifiable))
								.getObjectTerm())
				.ground();
		Call<Relation> call = Call.of(rel, image);
		if (rule != null) {
			return Long.MAX_VALUE;
		}
		return backend.fold(
				source -> source.estimate(call),
				producer -> producer.estimate(call));
	}

	private static Fiber<Array<Unifiable<?>>> substituteQueryItems(Substitutions s, Array<Unifiable<?>> query) {
		return query.toJavaStream()
				.map(u -> (Unifiable<?>) s.walk(u))
				.map(Stream::of)
				.map(Fiber::done)
				.reduce((acc, c) -> Fiber.zip(acc, c).map(t -> t.apply(Stream::concat)))
				.orElseGet(() -> Fiber.done(Stream.empty()))
				.map(q -> q.collect(Array.collector()));
	}

	@Override
	public String toString() {
		return rel.toString() + args;
	}
}
