package com.tgac.pldb.index;
import com.tgac.functional.Streams;
import io.vavr.Tuple2;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
@ToString
public class MutableIndex<K, V> implements Index<K, V> {
	@Getter
	@Setter
	private V value;
	private final HashMap<K, MutableIndex<K, V>> lookup = new HashMap<>();

	@Override
	public Option<Index<K, V>> get(K value) {
		return Option.of(lookup.get(value));
	}

	@Override
	public Option<Index<K, V>> get(IndexedSeq<K> seq) {
		return Option.ofOptional(getSeq(seq)
						.map(Streams.enumerateLong())
						.reduce((l, r) -> r))
				.filter(t -> t._1.intValue() == seq.length() - 1)
				.map(Tuple2::_2);
	}

	public java.util.stream.Stream<Index<K, V>> getSeq(IndexedSeq<K> seq) {
		return getSeq(Option.of(this), seq).toJavaStream();
	}

	public java.util.stream.Stream<MutableIndex<K, V>> createPath(IndexedSeq<K> seq) {
		return createPath(this, seq).toJavaStream();
	}

	public void remove(IndexedSeq<K> seq) {
		get(seq.dropRight(1))
				.toJavaOptional()
				.map(i -> (MutableIndex<K, V>) i)
				.ifPresent(i -> i.lookup.remove(seq.last()));
	}

	private static <K, V> Stream<Index<K, V>> getSeq(Option<Index<K, V>> idx, IndexedSeq<K> seq) {
		if (idx.isEmpty() || seq.isEmpty()) {
			return Stream.empty();
		} else {
			Option<Index<K, V>> next = idx.get().get(seq.head());
			return next
					.map(h -> Stream.cons(h,
							() -> getSeq(next, seq.tail())))
					.getOrElse(Stream::empty);
		}
	}

	private static <K, V> Stream<MutableIndex<K, V>> createPath(MutableIndex<K, V> idx, IndexedSeq<K> seq) {
		if (seq.isEmpty()) {
			return Stream.empty();
		} else {
			MutableIndex<K, V> head = getOrCreateIndex(idx, seq.head());
			return Stream.cons(head,
					() -> createPath(head, seq.tail()));
		}
	}

	private static <K, V> MutableIndex<K, V> getOrCreateIndex(MutableIndex<K, V> i, K key) {
		i.lookup.computeIfAbsent(key, k -> new MutableIndex<>());
		return i.lookup.get(key);
	}
}
