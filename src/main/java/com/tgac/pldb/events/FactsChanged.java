package com.tgac.pldb.events;
import com.tgac.pldb.relations.Fact;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@RequiredArgsConstructor(staticName = "of")
public class FactsChanged {
	List<Fact> added;
	List<Fact> removed;
}
