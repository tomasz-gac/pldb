package com.tgac.pldb;
import com.tgac.pldb.index.ImmutableIndex;
import com.tgac.pldb.index.Index;
import io.vavr.collection.Array;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
public class ImmutableIndexTest {

	@Test
	public void shouldPutAndGetBasic() {
		ImmutableIndex<Integer, Integer> index = ImmutableIndex.<Integer, Integer> of(0)
				.withIndexAt(Array.of(1), () -> 0/*, Integer::sum*/, i -> i.withValue(123));
		assertThat(index.get(Array.of(1)).map(Index::getValue).toJavaOptional())
				.hasValue(123);
		index = index.withIndexAt(
				Array.of(2, 3),
				() -> 0,
				i -> i.withValue(321));

		assertThat(index.get(Array.of(1)).map(Index::getValue).toJavaOptional())
				.hasValue(123);

		index = index.withIndexAt(Array.of(1, 2, 3, 4),
				() -> 0, i -> i.withValue(333));

		assertThat(index.get(Array.of(1, 2, 3, 4)).map(Index::getValue).toJavaOptional())
				.hasValue(333);

		index = index.withIndexAt(Array.of(1, 2, 3),
				() -> 0, /*Integer::sum,*/ i -> i.withValue(222));

		assertThat(index.get(Array.of(1, 2, 3)).map(Index::getValue).toJavaOptional())
				.hasValue(222);
	}

	@Test
	public void shouldPutAndGet() {
		ImmutableIndex<Integer, Integer> i = ImmutableIndex.of(0);
		Array<Integer> seq = Array.of(1, 2, 3, 4, 5);
		assertThat(i.get(seq).toJavaOptional())
				.isEmpty();
		i = i.withPath(seq, () -> 0);
		assertThat(i.get(seq))
				.isNotEmpty();
		i = i.withIndexAt(Array.of(1, 2, 3, 4),
				() -> -1,
				item -> item.withValue(123));
		assertThat(i.get(Array.of(1, 2, 3, 4)).get().getValue())
				.isEqualTo(123);
		assertThat(i.get(Array.empty()).get())
				.isEqualTo(i);
	}

	@Test
	public void shouldRemove() {
		ImmutableIndex<Integer, Integer> i = ImmutableIndex.of(-1);
		Array<Integer> seq = Array.of(1, 2, 3, 4, 5);
		assertThat(i.get(seq).toJavaOptional())
				.isEmpty();
		i = i.withIndexAt(seq,
				() -> -1,
				item -> item.withValue(123));
		assertThat(i.get(seq).get().getValue())
				.isEqualTo(123);
		i = i.remove(seq);
		assertThat(i.get(seq).toJavaOptional())
				.isEmpty();
		i = i.withIndexAt(seq.dropRight(1),
				() -> -1,
				v -> v.withValue(4));
		assertThat(i.get(seq.dropRight(1)).toJavaOptional())
				.hasValueSatisfying(v -> assertThat(v.getValue()).isEqualTo(4));
		assertThat(i.remove(Array.of(5, 7, 2)))
				.isEqualTo(i);
		i = i.remove(Array.empty());
		assertThat(i.getLookup().size())
				.isEqualTo(0);
	}

	@Test
	public void shouldPut() {
		ImmutableIndex<Integer, String> idx1 = ImmutableIndex.<Integer, String> of("first")
				.withIndexAt(Array.of(1, 2),
						() -> "first",
						j -> j.withValue("w"));

		Assertions.assertThat(idx1.get(Array.of(1, 2)).get().getValue())
				.isEqualTo("w");

		ImmutableIndex<Integer, String> idx2 = idx1
				.withIndexAt(Array.of(1, 3), () -> "first", j -> j.withValue("q"));
		System.out.println(idx2);

		Assertions.assertThat(idx2.get(Array.of(1, 2)).toJavaOptional().map(Index::getValue))
				.hasValue("w");

		Assertions.assertThat(idx2.get(Array.of(1, 3)).toJavaOptional().map(Index::getValue))
				.hasValue("q");
	}

	@Test
	public void shouldMerge() {
		ImmutableIndex<Integer, String> idx1 = ImmutableIndex.<Integer, String> of("first")
				.withIndexAt(Array.of(1, 2, 3, 4),
						() -> "first",
						i -> i.withValue("item1"));
		ImmutableIndex<Integer, String> idx2 = ImmutableIndex.<Integer, String> of("second")
				.withIndexAt(Array.of(1, 2, 5),
						() -> "second",
						i -> i.withValue("item2"));
		ImmutableIndex<Integer, String> merged = idx1.sum(idx2, (l, r) -> l + " " + r);

		Assertions.assertThat(merged.get(Array.of(1, 2, 3, 4))
						.map(Index::getValue).toJavaOptional())
				.hasValue("item1");
		Assertions.assertThat(merged.get(Array.of(1, 2, 5)).map(Index::getValue).toJavaOptional())
				.hasValue("item2");
		Assertions.assertThat(merged.get(Array.of(1)).map(Index::getValue).toJavaOptional())
				.hasValue("first second");
	}

}
