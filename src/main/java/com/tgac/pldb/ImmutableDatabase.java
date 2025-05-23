package com.tgac.pldb;
import com.tgac.functional.Exceptions;
import com.tgac.pldb.index.ImmutableIndex;
import com.tgac.pldb.index.Index;
import com.tgac.pldb.relations.Fact;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.stream.Stream;

@ToString
@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class ImmutableDatabase extends AbstractIndexedDatabase {
	public final ImmutableIndex<Tuple2<Integer, Object>, Set<Fact>> index;
	private final List<Trigger> triggers;

	public static ImmutableDatabase empty() {
		return of(ImmutableIndex.of(HashSet.empty()), List.empty());
	}

	@Override
	public Database withTrigger(Trigger trigger) {
		return ImmutableDatabase.of(index, triggers.prepend(trigger));
	}

	@Override
	public AbstractIndexedDatabase withFact(Fact fact) {
		return ImmutableDatabase.of(
				index.sum(
						createIndexPaths(fact)
								.reduce(ImmutableIndex.of(HashSet.empty()),
										(acc, seq) -> acc.withIndexAt(seq,
												HashSet::empty,
												ci -> ci.updateValue(v -> v.add(fact))),
										Exceptions.throwingBiOp(UnsupportedOperationException::new)),
						Set::addAll),
				triggers);
	}

	@Override
	protected AbstractIndexedDatabase withoutFact(Fact fact) {
		return ImmutableDatabase.of(
				createIndexPaths(fact)
						.reduce(this.index,
								(acc, seq) -> acc.withIndexAt(seq,
										Exceptions.throwingSupplier(UnsupportedOperationException::new),
										idx -> idx.updateValue(v -> v.remove(fact))),
								Exceptions.throwingBiOp(UnsupportedOperationException::new)),
				triggers);
	}
	@Override
	protected Stream<Trigger> getTriggers() {
		return triggers.toJavaStream();
	}

	@Override
	protected Iterable<Fact> extractDataFromIndex(Stream<Tuple2<Integer, Object>> indices) {
		return index.get(indices.collect(Array.collector()))
				.map(Index::getValue)
				.getOrElse(HashSet::empty);
	}
}
