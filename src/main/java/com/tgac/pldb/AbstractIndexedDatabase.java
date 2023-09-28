package com.tgac.pldb;
import com.tgac.functional.Streams;
import com.tgac.logic.LVal;
import com.tgac.logic.Unifiable;
import com.tgac.pldb.events.DatabaseEvent;
import com.tgac.pldb.events.FactChangedEvent;
import com.tgac.pldb.events.FactsChanged;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tgac.functional.exceptions.Exceptions.throwingBiOp;

@Slf4j
public abstract class AbstractIndexedDatabase implements Database {
	protected abstract Iterable<IndexedSeq<Object>> extractDataFromIndex(Stream<Tuple2<Integer, Object>> indices);

	protected abstract AbstractIndexedDatabase fact(Fact fact);

	protected abstract AbstractIndexedDatabase retract(Fact fact);

	protected abstract void notify(DatabaseEvent event);

	@SuppressWarnings("Convert2MethodRef")
	private Stream<IndexedSeq<Integer>> indexSelector(IndexedSeq<Integer> indexSeq) {
		return indexSeq
				.permutations()
				.toJavaStream()
				.flatMap(perm -> IntStream.range(0, indexSeq.size() + 1)
						.mapToObj(i -> perm.dropRight(i))
						.filter(AbstractIndexedDatabase::permutationIsInOrder))
				.distinct();
	}

	@Override
	public Validation<Seq<String>, Database> facts(List<Fact> facts) {
		AbstractIndexedDatabase result = facts.stream()
				.reduce(this,
						AbstractIndexedDatabase::fact,
						throwingBiOp(UnsupportedOperationException::new));

		facts.stream()
				.map(f -> FactsChanged.of(f, FactChangedEvent.FACT_ADDED))
				.forEach(this::notify);

		return Validation.valid(result);
	}

	@Override
	public Validation<Seq<String>, Database> retract(List<Fact> facts){
		AbstractIndexedDatabase result = facts.stream()
				.reduce(this,
						AbstractIndexedDatabase::retract,
						throwingBiOp(UnsupportedOperationException::new));

		facts.stream()
				.map(f -> FactsChanged.of(f, FactChangedEvent.FACT_REMOVED))
				.forEach(this::notify);

		return Validation.valid(result);
	}

	@Override
	public Iterable<IndexedSeq<Object>> get(Relation relation, IndexedSeq<Unifiable<?>> query) {
		Iterable<IndexedSeq<Object>> result = extractDataFromIndex(getIndices(relation, query));
		if (log.isDebugEnabled()) {
			log.debug("{}({}) -> {}", relation,
					query.toJavaStream()
							.map(Object::toString)
							.collect(Collectors.joining(", ")), result);
		}
		return result;
	}

	protected Stream<Array<Tuple2<Integer, Object>>> createIndexPaths(Fact fact) {
		Array<Tuple2<Integer, Object>> seq = getIndices(fact.getRelation(),
				fact.getValues().map(LVal::lval))
				.collect(Array.collector());
		return indexSelector(Array.range(0, seq.size()))
				.map(perm -> perm.toJavaStream()
						.map(seq::get)
						.collect(Array.collector()));
	}

	protected static Stream<Tuple2<Integer, Object>> getIndices(Relation relation, IndexedSeq<Unifiable<?>> query) {
		return Stream.concat(Stream.of(Tuple.of(-1, relation.getName())),
				indexableQueryArgs(query)
						.filter(t -> relation.getArgs()[t._1].isIndexed())
						.map(t -> t.map2(Object.class::cast)));
	}

	private static Stream<Tuple2<Integer, Object>> indexableQueryArgs(
			IndexedSeq<Unifiable<?>> query) {
		return query.toJavaStream()
				.map(Unifiable::getObjectUnifiable)
				.map(Streams.enumerate())
				.filter(u -> u._2.asVal().isDefined())
				.map(u -> u.map2(Unifiable::asVal).map2(Option::get));
	}

	private static boolean permutationIsInOrder(IndexedSeq<Integer> perm) {
		return perm.toJavaStream()
				.reduce(Option.of(-1),
						(acc, v) -> acc.flatMap(a -> Option.of(v).filter(vv -> vv > a)),
						throwingBiOp(UnsupportedOperationException::new))
				.isDefined();
	}

}
