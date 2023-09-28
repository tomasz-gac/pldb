package com.tgac.pldb;
import com.tgac.logic.LVal;
import com.tgac.pldb.events.DatabaseEvent;
import com.tgac.pldb.events.DatabaseEventListener;
import com.tgac.pldb.index.ImmutableIndex;
import com.tgac.pldb.index.Index;
import com.tgac.pldb.relations.Fact;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.HashSet;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.stream.Stream;

import static com.tgac.functional.exceptions.Exceptions.throwingBiOp;

@ToString
@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class ImmutableDatabase extends AbstractIndexedDatabase {
	public final ImmutableIndex<Tuple2<Integer, Object>, Set<IndexedSeq<Object>>> index;
	public final List<DatabaseEventListener> listeners;

	public static ImmutableDatabase empty() {
		return of(ImmutableIndex.of(HashSet.empty()), List.empty());
	}

	@Override
	public AbstractIndexedDatabase fact(Fact fact) {
		return ImmutableDatabase.of(
				index.sum(
						createIndexPaths(fact)
								.reduce(ImmutableIndex.of(HashSet.empty()),
										(acc, seq) -> acc.withIndexAt(seq,
												HashSet::empty,
												ci -> ci.updateValue(v ->
														v.add(fact.getValues()
																.map(Object.class::cast)))),
										throwingBiOp(UnsupportedOperationException::new)),
						Set::addAll), listeners);
	}

	@Override
	protected AbstractIndexedDatabase retract(Fact fact) {
		return ImmutableDatabase.of(
				index.remove(getIndices(fact.getRelation(), fact.getValues().map(LVal::lval))
						.collect(Array.collector())),
				listeners);
	}

	@Override
	protected Iterable<IndexedSeq<Object>> extractDataFromIndex(Stream<Tuple2<Integer, Object>> indices) {
		return index.get(indices.collect(Array.collector()))
				.map(Index::getValue)
				.getOrElse(HashSet::empty);
	}

	@Override
	public Database observe(DatabaseEventListener handler) {
		return ImmutableDatabase.of(index, listeners.prepend(handler));
	}

	@Override
	protected void notify(DatabaseEvent e) {
		listeners.forEach(l -> l.accept(e));
	}
}
