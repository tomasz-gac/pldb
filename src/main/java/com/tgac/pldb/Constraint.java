package com.tgac.pldb;
import com.tgac.functional.Functions;
import com.tgac.pldb.events.FactsChanged;

import java.util.Optional;
public interface Constraint extends Functions._2<FactsChanged, Database, Optional<String>> {
}
