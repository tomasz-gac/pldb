package com.tgac.pldb.relations;
import com.tgac.functional.recursion.Recur;
import com.tgac.logic.Goal;
import com.tgac.logic.LVal;
import com.tgac.logic.MiniKanren;
import com.tgac.logic.Unifiable;
import com.tgac.pldb.Database;
import io.vavr.collection.Array;
import lombok.Value;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tgac.logic.Incomplete.incomplete;
import static com.tgac.logic.LVal.lval;

@Value
public class RelationN implements Relation {
	String name;
	Property<?>[] args;

	public static RelationN of(String name, Property<?>... args) {
		return new RelationN(name, args);
	}

	@Override
	public Property<?>[] getArgs() {
		return args;
	}

	public Goal apply(Database db, Unifiable<?>... args) {
		return relation(db, this, args);
	}

	public static Goal relation(Database db, Relation rel, Unifiable<?>... args) {
		return s -> incomplete(() ->
				substituteQueryItems(s, Array.of(args))
						.map(q -> unifyQueryResults(db, rel, q).apply(s)));
	}

	private static Recur<Array<Unifiable<?>>> substituteQueryItems(MiniKanren.Substitutions s, Array<Unifiable<?>> query) {
		return query.toJavaStream()
				.map(u -> MiniKanren.walk(s, u).map(Stream::of))
				.reduce((acc, c) -> Recur.zip(acc, c).map(t -> t.apply(Stream::concat)))
				.orElseGet(() -> Recur.done(Stream.empty()))
				.map(q -> q.collect(Array.collector()));
	}

	private static Goal unifyQueryResults(Database db, Relation rel, Array<Unifiable<?>> query) {
		return StreamSupport.stream(db.get(rel, query).spliterator(), false)
				.map(seq -> lval(seq.map(Object.class::cast).map(LVal::lval)).unify(
						query.map(Unifiable::getObjectUnifiable)))
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
	}

	public Fact apply(Object... vs) {
		return Fact.of(this, Array.of(vs));
	}
}