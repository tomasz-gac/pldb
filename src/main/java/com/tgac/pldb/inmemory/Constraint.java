package com.tgac.pldb.inmemory;

import com.tgac.functional.Exceptions;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.LVal;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.relations.Answers;
import io.vavr.Function2;
import com.tgac.pldb.inmemory.events.ChangeType;
import com.tgac.pldb.inmemory.events.FactsChanged;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Property;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Constraint extends Function2<FactsChanged, Database, Optional<String>> {

	static Constraint unique(Relation relation, Property<?> property) {
		return unique(relation, Collections.singletonList(property));
	}

	static Constraint unique(Relation relation, List<Property<?>> properties) {
		IndexedSeq<Integer> propertyIndex = properties.stream()
				.map(p -> relation.indexOf(p)
						.orElseThrow(Exceptions.format(IllegalArgumentException::new,
								"No such property %s in relation %s", relation, p)))
				.collect(Array.collector());

		return (fc, db) ->
				Optional.of(fc.getFacts().stream())
						.filter(__ -> fc.getChange() == ChangeType.ADDED)
						.flatMap(facts -> facts
								.filter(f -> relation.equals(f.getRelation()))
								.findFirst()
								.map(i -> scan(db, probe(relation, -1, null))
										.map(fact -> propertyIndex.toJavaStream()
												.map(fact.getValues()::get)
												.collect(Collectors.toList()))
										.collect(Collectors.toList()))
								.map(l -> Tuple.of(l, new HashSet<>(l)))
								.filter(l -> l._1.size() != l._2.size())
								.map(l -> "Unique ids required for relation " + relation.getName() + ": " + properties));
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
										Optional.of(String.format("Foreign key property %s of %s has no matching property %s of %s for fact: %s",
												fkProperty, fkRelation, pkProperty, pkRelation, j._1)) :
										Optional.<String> empty()) :
						getJoinResults(db,
								fc.getFacts().stream(),
								pkRelation, pkProperty,
								fkRelation, fkProperty)
								.map(j -> !j._2.isEmpty() ?
										Optional.of(String.format("Foreign key property %s of %s has no matching property %s of %s for fact: %s",
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
		return scan(db, probe(relation, targetPropertyIndex, value));
	}

	/** One bound position ({@code -1} = none), the rest free. */
	static Call<Relation> probe(Relation relation, int boundIndex, Object value) {
		List<Object> members = new ArrayList<>();
		int frees = 0;
		for (int i = 0; i < relation.getArgs().length; i++) {
			members.add(i == boundIndex ? LVal.lval(value) : Any.of(frees++));
		}
		return Call.of(relation, (Reified<?>) LVal.lval(Array.ofAll(members)));
	}

	/** The integrity tier reads GROUND rows: answers decoded back to facts. */
	static Stream<Fact> scan(Database db, Call<Relation> probe) {
		return StreamSupport.stream(db.answers(probe).spliterator(), false)
				.map(answer -> Fact.of(probe.getRelation(), Answers.values(answer._1)));
	}
}
