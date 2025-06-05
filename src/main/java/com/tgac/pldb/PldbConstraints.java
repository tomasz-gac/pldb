package com.tgac.pldb;

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.monad.Cont;
import com.tgac.logic.ckanren.ConstraintStore;
import com.tgac.logic.ckanren.StoreSupport;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.LVal;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.MiniKanren;
import com.tgac.logic.unification.Package;
import com.tgac.logic.unification.Store;
import com.tgac.logic.unification.Stored;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.control.Option;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class PldbConstraints implements ConstraintStore {
	@Value
	@RequiredArgsConstructor(staticName = "of")
	public static class StoredRelation implements Stored {
		Database db;
		Relation relation;
		Array<Unifiable<?>> vars;

		@Override
		public Class<? extends Store> getStoreClass() {
			return PldbConstraints.class;
		}
	}

	private static final PldbConstraints EMPTY = new PldbConstraints(LinkedHashSet.empty());

	LinkedHashSet<StoredRelation> relations;

	public static Goal exists(Database db, Relation rel, Unifiable<?>[] args) {
		return Goal.goal(s -> Cont.just(s.withStore(EMPTY)))
				.and(s -> Cont.just(StoreSupport.withConstraint(s,
						StoredRelation.of(db, rel, Array.of(args)))));
	}

	@Override
	public <T> Goal enforceConstraints(Unifiable<T> x) {
		return Goal.goal(s -> relationsSortedByCount(s)
				.map(r -> forceUnify(r.db, r.relation, r.vars.toJavaArray(Unifiable[]::new)))
				.reduce(Goal.success(), Goal::and)
				.apply(s));
	}

	@Override
	public Goal processPrefix(HashMap<LVar<?>, Unifiable<?>> newSubstitutions) {
		return s -> Cont.just(s.withSubstitutions(newSubstitutions));
	}

	@Override
	public <A> Unifiable<A> reify(Unifiable<A> unifiable, Package renameSubstitutions, Package p) {
		return unifiable;
	}

	@Override
	public Store remove(Stored c) {
		if (c instanceof StoredRelation) {
			return new PldbConstraints(relations.remove((StoredRelation) c));
		} else {
			return this;
		}
	}

	@Override
	public Store prepend(Stored c) {
		if (c instanceof StoredRelation) {
			return new PldbConstraints(relations.add((StoredRelation) c));
		} else {
			return this;
		}
	}

	@Override
	public boolean contains(Stored c) {
		if (c instanceof StoredRelation) {
			return relations.contains((StoredRelation) c);
		} else {
			return false;
		}
	}

	private static Goal forceUnify(Database db, Relation rel, Unifiable<?>... args) {
		return s -> unifyQueryResults(db, rel,
				substituteQueryItems(s, Array.of(args)))
				.apply(s);
	}

	private static int count(Package s, Database db, Relation rel, Unifiable<?>... args) {
		return db.count(rel,
				substituteQueryItems(s, Array.of(args))
						.map(Unifiable::getObjectUnifiable)
						.map(Unifiable::asVal)
						.map(Option::toJavaOptional));
	}

	private static Array<Unifiable<?>> substituteQueryItems(Package s, Array<Unifiable<?>> query) {
		return query.toJavaStream()
				.map(u -> MiniKanren.walk(s, u))
				.map(Stream::of)
				.reduce(Stream::concat)
				.orElseGet(Stream::empty)
				.collect(Array.collector());
	}

	private static Goal unifyQueryResults(Database db, Relation rel, Array<Unifiable<?>> query) {
		return StreamSupport.stream(db.get(rel, query
								.map(Unifiable::getObjectUnifiable)
								.map(Unifiable::asVal)
								.map(Option::toJavaOptional))
						.spliterator(), false)
				.map(fact -> lval(fact.getValues().map(Object.class::cast).map(LVal::lval))
						.unifies(query.map(Unifiable::getObjectUnifiable)))
				.reduce(Goal::or)
				.orElseGet(Goal::failure);
	}

	private Stream<StoredRelation> relationsSortedByCount(Package s) {
		return relations.toJavaStream()
				.map(r -> Tuple.of(r, count(s, r.db, r.relation, r.vars.toJavaArray(Unifiable[]::new))))
				.sorted(Comparator.comparing(Tuple2::_2))
				.map(Tuple2::_1);
	}
}
