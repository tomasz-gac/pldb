package com.tgac.pldb;
import com.tgac.functional.Functions;
import com.tgac.functional.Exceptions;
import com.tgac.pldb.events.ChangeType;
import com.tgac.pldb.events.FactsChanged;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
public interface Constraint extends Functions._2<FactsChanged, Database, Optional<String>> {
	static Constraint unique(Relation relation, Property<?> property) {
		Integer propertyIndex = relation.indexOf(property)
				.orElseThrow(Exceptions.format(IllegalArgumentException::new,
						"No such property %s in relation %s", relation, property));

		return (fc, db) ->
				Optional.of(fc.getFacts().stream())
						.filter(__ -> fc.getChange() == ChangeType.ADDED)
						.flatMap(facts -> facts
								.filter(f -> relation.equals(f.getRelation()))
								.findFirst()
								.map(i -> StreamSupport.stream(db.get(relation, Array.empty()).spliterator(), false)
										.map(s -> s.getValues().get(propertyIndex))
										.collect(Collectors.toList()))
								.map(l -> Tuple.of(l, new HashSet<>(l)))
								.filter(l -> l._1.size() != l._2.size())
								.map(l -> "Unique ids required for relation person."));
	}

	static <T> Constraint foreignKey(
			Relation fkRelation, Property<T> fkProperty,
			Relation pkRelation, Property<T> pkProperty) {
		return (fc, db) ->
				(fc.getChange() == ChangeType.ADDED ?
						getJoinResults(db,
								fc.getFacts().stream(),
								fkRelation, fkProperty,
								pkRelation, pkProperty)
								.map(j -> j._2.isEmpty() ?
										Optional.of(format("Foreign key property %s of %s has no matching property %s of %s for fact: %s",
												fkProperty, fkRelation, pkProperty, pkRelation, j._1)) :
										Optional.<String> empty()) :
						getJoinResults(db,
								fc.getFacts().stream(),
								pkRelation, pkProperty,
								fkRelation, fkProperty)
								.map(j -> !j._2.isEmpty() ?
										Optional.of(format("Foreign key property %s of %s has no matching property %s of %s for fact: %s",
												fkProperty, fkRelation, pkProperty, pkRelation, j._1)) :
										Optional.<String> empty()))
						.filter(Optional::isPresent)
						.findFirst()
						.orElseGet(Optional::empty);
	}

	static <T> Stream<Tuple2<Fact, List<Fact>>> getJoinResults(
			Database db,
			Stream<Fact> facts,
			Relation relation,
			Property<T> property,
			Relation joinedRelation,
			Property<T> joinedProperty) {
		return facts
				.filter(f -> relation.equals(f.getRelation()))
				.map(f -> join(db, f, relation, property, joinedRelation, joinedProperty));
	}

	static <T> Tuple2<Fact, List<Fact>> join(
			Database db, Fact f,
			Relation relation,
			Property<T> property,
			Relation joinedRelation,
			Property<T> joinedProperty) {
		return Tuple.of(f,
				findUsingKey(db, joinedRelation, joinedProperty,
						f.get(property)
								.orElseThrow(Exceptions.format(IllegalArgumentException::new,
										"No such property %s in relation %s", property, relation)))
						.map(fact -> Fact.of(relation, fact.getValues().toArray()))
						.collect(Collectors.toList()));
	}

	static Stream<Fact> findUsingKey(Database db, Relation relation, Property<?> property, Object value) {
		Integer targetPropertyIndex = relation.indexOf(property)
				.orElseThrow(Exceptions.format(IllegalArgumentException::new,
						"No such property %s in relation %s", property, relation));

		return StreamSupport.stream(db.get(relation,
						Array.fill(relation.getArgs().length, Optional.empty())
								.update(targetPropertyIndex, Optional.of(value))).spliterator(),
				false);
	}
}
