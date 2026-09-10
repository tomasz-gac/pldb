package com.tgac.pldb.inmemory.events;

import com.tgac.pldb.relations.Fact;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "of")
public class FactsChanged {
	ChangeType change;
	List<Fact> facts;
}
