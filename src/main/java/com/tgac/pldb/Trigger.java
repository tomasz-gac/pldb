package com.tgac.pldb;
import io.vavr.Function2;
import com.tgac.pldb.events.FactsChanged;
import io.vavr.control.Try;
public interface Trigger extends Function2<FactsChanged, Database, Try<Database>> {
}
