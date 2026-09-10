package com.tgac.pldb.inmemory;

import com.tgac.pldb.inmemory.events.FactsChanged;
import io.vavr.Function2;
import io.vavr.control.Try;

public interface Trigger extends Function2<FactsChanged, Database, Try<Database>> {
}
