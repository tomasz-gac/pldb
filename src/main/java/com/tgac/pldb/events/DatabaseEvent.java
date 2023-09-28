package com.tgac.pldb.events;
import java.util.Optional;
public interface DatabaseEvent {
	Optional<FactsChanged> asFactsChanged();
}
