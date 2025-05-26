package com.tgac.pldb;

import com.tgac.functional.monad.Cont;
import com.tgac.logic.ckanren.ConstraintStore;
import com.tgac.logic.ckanren.PackageAccessor;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.Package;
import com.tgac.logic.unification.Store;
import com.tgac.logic.unification.Stored;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.relations.RelationN;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class PldbConstraintStore implements ConstraintStore {
	private static final PldbConstraintStore EMPTY = new PldbConstraintStore(HashSet.empty());

	HashSet<StoredRelation> relations;

	public static Goal pldbConstraint() {
		return s -> Cont.just(register(s));
	}

	public static Package register(Package p) {
		return p.withStore(EMPTY);
	}

	@Override
	public <T> Goal enforceConstraints(Unifiable<T> x) {
		return relations.toJavaStream()
				//				.filter(r -> r.vars.contains(x))
				.map(r -> RelationN.rel(r.db, r.relation, r.vars.toJavaArray(Unifiable[]::new)))
				.reduce(Goal.success(), Goal::and);
	}

	@Override
	public PackageAccessor processPrefix(HashMap<LVar<?>, Unifiable<?>> newSubstitutions) {
		return PackageAccessor.identity();
	}

	@Override
	public <A> Unifiable<A> reify(Unifiable<A> unifiable, Package renameSubstitutions, Package p) {
		return unifiable;
	}

	@Override
	public Store remove(Stored c) {
		if (c instanceof StoredRelation) {
			return new PldbConstraintStore(relations.remove((StoredRelation) c));
		} else {
			return this;
		}
	}

	@Override
	public Store prepend(Stored c) {
		if (c instanceof StoredRelation) {
			return new PldbConstraintStore(relations.add((StoredRelation) c));
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

	@Value
	@RequiredArgsConstructor
	public static class StoredRelation implements Stored {
		Database db;
		Relation relation;
		Array<Unifiable<?>> vars;

		@Override
		public Class<? extends Store> getStoreClass() {
			return PldbConstraintStore.class;
		}
	}
}
