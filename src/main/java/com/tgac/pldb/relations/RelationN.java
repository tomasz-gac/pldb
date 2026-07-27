package com.tgac.pldb.relations;


import com.tgac.logic.goals.Goal;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.Database;
import com.tgac.pldb.constraints.TableConstraints;
import io.vavr.collection.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RelationN implements Relation {
	String name;
	Property<?>[] args;

	public static RelationN of(String name, Property<?>... args) {
		validateProperties(args);
		return new RelationN(name, args);
	}

	public static void validateProperties(Property<?>... args) {
		List<String> duplicates = Arrays.stream(args)
				.collect(Collectors.groupingBy(Property::getName))
				.entrySet()
				.stream()
				.filter(e -> e.getValue().size() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
		if(!duplicates.isEmpty()){
			throw new IllegalArgumentException("Duplicated property names: " + duplicates);
		}
	}

	@Override
	public Property<?>[] getArgs() {
		return args;
	}

	public Goal apply(Database db, Unifiable<?>... args) {
		return relation(db, this, args);
	}

	public static Goal relation(Database db, Relation rel, Unifiable<?>... args) {
		return LookupGoal.of(db, rel, Array.of(args));
	}

	public Goal posted(Database db, Unifiable<?>... args) {
		return posted(db, this, args);
	}

	public static Goal posted(Database db, Relation rel, Unifiable<?>... args) {
		return TableConstraints.posted(db, rel, Array.of(args));
	}

	public Fact apply(Object... vs) {
		return Fact.of(this, Array.of(vs));
	}
}