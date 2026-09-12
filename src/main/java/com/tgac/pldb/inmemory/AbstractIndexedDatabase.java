package com.tgac.pldb.inmemory;

import static com.tgac.functional.Exceptions.throwingBiOp;

import com.tgac.functional.Streams;
import com.tgac.logic.tabling.Call;
import com.tgac.logic.unification.LVal;
import com.tgac.logic.unification.Term;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.inmemory.events.ChangeType;
import com.tgac.pldb.inmemory.events.FactsChanged;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractIndexedDatabase implements Database {

	protected abstract Iterable<Fact> extractDataFromIndex(Stream<Tuple2<Integer, Object>> indices);

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
	public Iterable<Answer> answers(Call<Relation> probe) {
		// the index speaks Term vocabulary directly: a ground position keys
		// its (indexed) bucket — null included, a null cell built a null
		// bucket at insert — a free position selects nothing; non-indexed
		// bound positions over-deliver and the consumer's restate filters.
		// The probe's RESIDUES are ignored on the same license: constraint
		// knowledge about free positions could only SHRINK the bucket, and
		// every extra row dies where the region would have killed it — the
		// restate resolves through the chokepoint, where the caller's own
		// stores veto. The SQL tier compiles residues into the WHERE because
		// its rows cross a wire; here over-delivery costs a walk, not a fetch
		Relation relation = probe.getRelation();
		Iterable<Fact> bucket = extractDataFromIndex(getIndices(relation,
				Answers.positions(probe.getArguments())));
		List<Answer> rows = StreamSupport.stream(bucket.spliterator(), false)
				.map(Answers::answer)
				.collect(Collectors.toList());
		if (log.isDebugEnabled()) {
			log.debug("{}{} -> {}", relation, probe.getArguments(), rows);
		}
		return rows;
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

	protected static Stream<Tuple2<Integer, Object>> getIndices(Relation relation, IndexedSeq<? extends Term<?>> query) {
		return Stream.concat(Stream.of(Tuple.of(-1, relation.getName())),
				indexableQueryArgs(query)
						.filter(t -> relation.getArgs()[t._1].isIndexed())
						.map(t -> t.map2(Object.class::cast)));
	}

	/** Ground positions key the index — the query only needs Term's asVal. */
	private static Stream<Tuple2<Integer, Object>> indexableQueryArgs(
			IndexedSeq<? extends Term<?>> query) {
		return query.toJavaStream()
				.map(Term::getObjectTerm)
				.map(Streams.enumerate())
				.filter(u -> u._2.asVal().isDefined())
				.map(u -> u.map2(Term::asVal).map2(Option::get));
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
