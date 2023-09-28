package com.tgac.pldb.events;
import com.tgac.pldb.relations.Fact;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Optional;

@Value
@RequiredArgsConstructor(staticName = "of")
public class FactsChanged implements DatabaseEvent {
	Fact fact;
	FactChangedEvent event;
	@Override
	public Optional<FactsChanged> asFactsChanged() {
		return Optional.of(this);
	}
}
