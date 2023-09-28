package com.tgac.pldb;
import com.tgac.functional.Streams;
import com.tgac.pldb.index.MutableIndex;
import io.vavr.collection.Array;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
public class MutableIndexTest {

	@Test
	public void shouldPutAndGet() {
		MutableIndex<Integer, Integer> i = new MutableIndex<>();
		Array<Integer> seq = Array.of(1, 2, 3, 4, 5);
		Assertions.assertThat(i.get(seq).toJavaOptional())
				.isEmpty();
		List<MutableIndex<Integer, Integer>> path = i.createPath(seq)
				.collect(Collectors.toList());
		Assertions.assertThat(i.get(seq).toJavaOptional())
				.isNotEmpty();
		path.get(path.size() - 1).setValue(123);
		Assertions.assertThat(i.get(seq).get().getValue())
				.isEqualTo(123);
	}

	@Test
	public void shouldRemove() {
		MutableIndex<Integer, Integer> i = new MutableIndex<>();
		Array<Integer> seq = Array.of(0, 1, 2, 3, 4);
		Assertions.assertThat(i.get(seq).toJavaOptional())
				.isEmpty();
		i.createPath(seq)
				.map(Streams.enumerateLong())
				.forEach(j -> j._2.setValue(j._1.intValue()));
		Assertions.assertThat(i.get(seq).get().getValue())
				.isEqualTo(4);
		i.remove(Array.of(0, 1, 2));
		Assertions.assertThat(i.get(seq).toJavaOptional())
				.isEmpty();
		Assertions.assertThat(i.get(Array.of(0, 1)).get().getValue())
				.isEqualTo(1);

	}

}
