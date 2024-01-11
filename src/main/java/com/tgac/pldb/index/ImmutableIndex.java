package com.tgac.pldb.index;
import com.tgac.functional.Streams;
import com.tgac.functional.Exceptions;
import com.tgac.functional.recursion.Recur;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.tgac.functional.recursion.Recur.done;
import static com.tgac.functional.recursion.Recur.recur;
@Value
@RequiredArgsConstructor(staticName = "of")
public class ImmutableIndex<K, V> implements Index<K, V> {
	V value;
	HashMap<K, ImmutableIndex<K, V>> lookup;

	public static <K, V> ImmutableIndex<K, V> of(V value) {
		return of(value, HashMap.empty());
	}

	@Override
	public Option<Index<K, V>> get(K value) {
		return lookup.get(value).map(i -> i);
	}

	public ImmutableIndex<K, V> withValue(V value) {
		return ImmutableIndex.of(value, lookup);
	}

	public ImmutableIndex<K, V> updateValue(UnaryOperator<V> update) {
		return withValue(update.apply(getValue()));
	}

	public ImmutableIndex<K, V> put(K key, ImmutableIndex<K, V> index) {
		return ImmutableIndex.of(value, lookup.put(key, index));
	}

	@Override
	public Option<Index<K, V>> get(IndexedSeq<K> seq) {
		if (seq.isEmpty()) {
			return Option.of(this);
		}
		return Option.ofOptional(getSeq(seq).toJavaStream()
						.map(Streams.enumerate())
						.reduce((l, r) -> r))
				.filter(t -> t._1 == seq.length() - 1)
				.map(Tuple2::_2)
				.map(Tuple2::_2);
	}

	public ImmutableIndex<K, V> sum(ImmutableIndex<K, V> other, BinaryOperator<V> valueMerger) {
		return ImmutableIndex.of(
				valueMerger.apply(value, other.value),
				sum(lookup, other.lookup, valueMerger).get());
	}

	private static <K, V> Recur<HashMap<K, ImmutableIndex<K, V>>> sum(
			HashMap<K, ImmutableIndex<K, V>> lhs,
			HashMap<K, ImmutableIndex<K, V>> rhs,
			BinaryOperator<V> valueMerger) {
		if (lhs.isEmpty()) {
			return done(rhs);
		} else if (rhs.isEmpty()) {
			return done(lhs);
		} else {
			return rhs.foldLeft(
					done(lhs),
					(result, entry) -> {
						K rhsKey = entry._1;
						ImmutableIndex<K, V> rhsValue = entry._2;
						return result.flatMap(r -> r.get(rhsKey)
								.map(resultValue -> recur(() ->
										sum(resultValue.lookup, rhsValue.lookup, valueMerger))
										.map(v -> r.put(rhsKey,
												ImmutableIndex.of(
														valueMerger.apply(resultValue.value, rhsValue.value),
														v))))
								.getOrElse(() -> result.map(
										resultIndex -> resultIndex.put(rhsKey, rhsValue))));
					});
		}
	}

	public ImmutableIndex<K, V> withPath(IndexedSeq<K> seq, Supplier<V> defaultValue) {
		return withIndexAt(seq, defaultValue, i -> i);
	}

	public ImmutableIndex<K, V> withIndexAt(
			IndexedSeq<K> seq,
			Supplier<V> defaultValue,
			UnaryOperator<ImmutableIndex<K, V>> update) {
		if (seq.isEmpty()) {
			return update.apply(this);
		}
		Array<Tuple2<K, ImmutableIndex<K, V>>> indices =
				getOrCreatePath(this, seq, defaultValue)
						.toJavaStream()
						.collect(Array.collector())
						.reverse();
		Tuple2<K, ImmutableIndex<K, V>> updatedHead = indices.head().map2(update);
		Tuple2<K, ImmutableIndex<K, V>> itemToInsert = indices.toJavaStream()
				.skip(1)
				.reduce(updatedHead,
						(acc, c) -> c.map2(prev -> acc.apply(prev::put)),
						Exceptions.throwingBiOp(UnsupportedOperationException::new));
		return ImmutableIndex.of(value, itemToInsert.apply(lookup::put));
	}

	public ImmutableIndex<K, V> remove(K key) {
		return ImmutableIndex.of(value, lookup.remove(key));
	}

	public ImmutableIndex<K, V> remove(IndexedSeq<K> seq) {
		if (seq.isEmpty()) {
			return ImmutableIndex.of(value);
		} else if (get(seq).isDefined()) {

			return withIndexAt(seq.dropRight(1),
					Exceptions.throwingSupplier(UnsupportedOperationException::new),
					i -> i.remove(seq.last()));
		} else {
			return this;
		}
	}

	private static <K, V> Stream<Tuple2<K, Index<K, V>>> getSeq(Option<Index<K, V>> idx, IndexedSeq<K> seq) {
		if (idx.isEmpty() || seq.isEmpty()) {
			return Stream.empty();
		} else {
			Option<Index<K, V>> next = idx.get().get(seq.head());
			return next
					.map(h -> Stream.cons(Tuple.of(seq.head(), h),
							() -> getSeq(next, seq.tail())))
					.getOrElse(Stream::empty);
		}
	}

	private static <K, V> Stream<Tuple2<K, ImmutableIndex<K, V>>> getOrCreatePath(
			ImmutableIndex<K, V> idx,
			IndexedSeq<K> seq,
			Supplier<V> defaultValue) {
		if (seq.isEmpty()) {
			return Stream.empty();
		} else {
			K key = seq.head();
			ImmutableIndex<K, V> head = getOrCreateIndex(idx, key, defaultValue);
			return Stream.cons(Tuple.of(key, head),
					() -> getOrCreatePath(head, seq.tail(), defaultValue));
		}
	}

	private Stream<Tuple2<K, Index<K, V>>> getSeq(IndexedSeq<K> seq) {
		return getSeq(Option.of(this), seq);
	}

	private static <K, V> ImmutableIndex<K, V> getOrCreateIndex(
			ImmutableIndex<K, V> i,
			K key,
			Supplier<V> defaultValue) {
		return i.lookup
				.computeIfAbsent(key, k -> ImmutableIndex.of(defaultValue.get()))
				._2.get(key)
				.get();
	}
}
