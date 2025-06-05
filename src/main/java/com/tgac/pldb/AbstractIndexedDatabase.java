package com.tgac.pldb;
import com.tgac.functional.Streams;
import com.tgac.logic.unification.LVal;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.events.ChangeType;
import com.tgac.pldb.events.FactsChanged;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tgac.functional.Exceptions.throwingBiOp;

@Slf4j
public abstract class AbstractIndexedDatabase implements Database {

	protected abstract Iterable<Fact> extractDataFromIndex(Stream<Tuple2<Integer, Object>> indices);
	protected abstract int count(Stream<Tuple2<Integer, Object>> indices);

	protected abstract AbstractIndexedDatabase withFact(Fact fact);

	protected abstract AbstractIndexedDatabase withoutFact(Fact fact);

	protected abstract Stream<Trigger> getTriggers();

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
	public Try<Database> withFacts(List<Fact> facts) {
		Database updated = facts.stream()
				.reduce(this,
						AbstractIndexedDatabase::withFact,
						throwingBiOp(UnsupportedOperationException::new));

		return processTriggers(updated,
				FactsChanged.of(ChangeType.ADDED, facts));
	}

	@Override
	public Try<Database> withoutFacts(List<Fact> facts) {
		Database updated = facts.stream()
				.reduce(this,
						AbstractIndexedDatabase::withoutFact,
						throwingBiOp(UnsupportedOperationException::new));

		return processTriggers(updated,
				FactsChanged.of(ChangeType.REMOVED, facts));
	}

	@Override
	public Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> query) {
		Iterable<Fact> result = extractDataFromIndex(getIndices(relation, query.map(o -> o.map(LVal::lval).orElseGet(LVar::lvar))));
		if (log.isDebugEnabled()) {
			log.debug("{}({}) -> {}", relation,
					query.toJavaStream()
							.map(Object::toString)
							.collect(Collectors.joining(", ")), result);
		}
		return result;
	}

	@Override
	public int count(Relation relation, IndexedSeq<Optional<Object>> query) {
		int count = count(getIndices(relation, query.map(o -> o.map(LVal::lval).orElseGet(LVar::lvar))));
		if (log.isDebugEnabled()) {
			log.debug("count({}({})) = {}", relation,
					query.toJavaStream()
							.map(Object::toString)
							.collect(Collectors.joining(", ")), count);
		}
		return count;
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

	private Try<Database> processTriggers(Database updated, FactsChanged event) {
		return getTriggers()
				.reduce(Try.success(updated),
						(db, trigger) -> db.flatMap(v -> trigger.apply(event, v)),
						throwingBiOp(UnsupportedOperationException::new));
	}
}
