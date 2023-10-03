package com.tgac.pldb;
import com.tgac.logic.LVal;
import com.tgac.pldb.index.Index;
import com.tgac.pldb.index.MutableIndex;
import com.tgac.pldb.relations.Fact;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import io.vavr.control.Option;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@ToString
public class MutableDatabase extends AbstractIndexedDatabase {
	private final MutableIndex<Tuple2<Integer, Object>, Set<IndexedSeq<Object>>> index = new MutableIndex<>();
	private final List<Trigger> triggers = new ArrayList<>();

	@Override
	protected AbstractIndexedDatabase withFact(Fact f) {
		createIndexPaths(f)
				.flatMap(index::createPath)
				.forEach(index ->
						Option.of(index.getValue())
								.orElse(() -> Option.of(new HashSet<>()))
								.peek(s -> s.add(f.getValues().map(Object.class::cast)))
								.toJavaOptional()
								.ifPresent(index::setValue));

		return this;
	}

	@Override
	protected AbstractIndexedDatabase withoutFact(Fact fact) {
		index.remove(getIndices(fact.getRelation(), fact.getValues()
				.map(LVal::lval))
				.collect(Array.collector()));
		return this;
	}
	@Override
	protected Stream<Trigger> getTriggers() {
		return triggers.stream();
	}

	@Override
	protected Iterable<IndexedSeq<Object>> extractDataFromIndex(Stream<Tuple2<Integer, Object>> indices) {
		return index.get(indices.collect(Array.collector()))
				.map(Index::getValue)
				.getOrElse(Collections::emptySet);
	}
	@Override
	public Database withTrigger(Trigger trigger) {
		triggers.add(trigger);
		return this;
	}
}
