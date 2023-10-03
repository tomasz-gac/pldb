package com.tgac.pldb;
import com.tgac.functional.Functions;
import com.tgac.pldb.events.FactsChanged;
import io.vavr.control.Try;
public interface Trigger extends Functions._2<FactsChanged, Database, Try<Database>> {
}
