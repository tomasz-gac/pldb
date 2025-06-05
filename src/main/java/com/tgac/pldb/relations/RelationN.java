package com.tgac.pldb.relations;

import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import com.tgac.pldb.PldbConstraints;
import io.vavr.collection.Array;
import lombok.Value;

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
		return PldbConstraints.exists(db, rel, args);
	}

	public Fact apply(Object... vs) {
		return Fact.of(this, Array.of(vs));
	}
}