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
import com.tgac.logic.unification.Term;
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
@RequiredArgsConstructor(access = AccessLevel.MODULE)
public class Literal implements Goal, Bounded, Postable {
	Either<AnswerSource, AnswerProducer> backend;
	Relation rel;
	Array<Unifiable<?>> args;

	/**
	 * The function-shaped front door: columns first, backend last. One
	 * {@link Builder#arg} per column — names stated once, arity unbounded,
	 * the defining method's signature the only typed surface — with
	 * {@link Builder#indexed()}/{@link Builder#ground()} as TAIL modifiers
	 * on the last column, freely composed. The terminal picks the reading:
	 * {@link Builder#from} enumerates a source, {@link Builder#produced}
	 * streams a producer, {@link Builder#solving} tables a rule in the
	 * solve — and a half-built literal is not a goal, so applying one is
	 * unrepresentable. Relation identity is by value (name plus columns):
	 * every mint of one definition is the same relation.
	 */
	public static Builder relation(String name) {
		return new Builder(name);
	}

	public static final class Builder {
		private final String name;
		private final java.util.List<Property<?>> columns = new java.util.ArrayList<>();
		private final java.util.List<Unifiable<?>> values = new java.util.ArrayList<>();

		private Builder(String name) {
			this.name = name;
		}

		public Builder arg(String column, Unifiable<?> value) {
			columns.add(Property.of(column));
			values.add(value);
			return this;
		}

		/** Marks the LAST declared column; refuses before any column. */
		public Builder indexed() {
			return modify(Property::indexed);
		}

		/**
		 * Marks the LAST declared column as an input: its argument must walk
		 * to a ground value when the literal applies, refused loudly
		 * otherwise.
		 */
		public Builder ground() {
			return modify(Property::ground);
		}

		private Builder modify(java.util.function.UnaryOperator<Property<?>> flag) {
			if (columns.isEmpty()) {
				throw new IllegalStateException(
						"relation '" + name + "': a modifier needs a column — declare arg() first");
			}
			columns.set(columns.size() - 1, flag.apply(columns.get(columns.size() - 1)));
			return this;
		}

		private Relation relation() {
			return RelationN.of(name, columns.toArray(new Property<?>[0]));
		}

		public Literal from(AnswerSource source) {
			return new Literal(Either.left(source), relation(), Array.ofAll(values));
		}

		public Literal produced(AnswerProducer producer) {
			return new Literal(Either.right(producer), relation(), Array.ofAll(values));
		}

		public Literal solving(Goal body) {
			Array<Unifiable<?>> heads = Array.ofAll(values);
			Relation rek = relation();
			return new Literal(Either.right(GoalProducer.of(rek, body, heads, Table.empty())), rek, heads);
		}
	}


	/** The imposition reading — under {@code exclude} this is the only one. */
	@Override
	public Posting posted() {
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
		requireGroundColumns(s);
		Goal rule = ownRule();
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

	/**
	 * The native reading: when the backend is our own rule producer speaking
	 * THESE variables (the builder's mint), the goal side consumes through
	 * the solve's shared table — recursion is an ordinary reader and rings
	 * seal. Any other producer, or a foreign pairing of this producer with
	 * different args, streams through produce.
	 */
	private Goal ownRule() {
		return backend.fold(
				source -> null,
				producer -> producer instanceof GoalProducer
						&& ((GoalProducer) producer).getHeads().equals(args)
						? ((GoalProducer) producer).getRule()
						: null);
	}

	/** A ground-marked column is an input: free at application is a caller error. */
	private void requireGroundColumns(Package s) {
		Property<?>[] cols = rel.getArgs();
		for (int i = 0; i < cols.length; i++) {
			if (cols[i].isGround() && !((Term<?>) s.walk(args.get(i))).asVal().isDefined()) {
				throw new IllegalStateException(rel.getName()
						+ ": ground column '" + cols[i].getName() + "' is unbound at application");
			}
		}
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
