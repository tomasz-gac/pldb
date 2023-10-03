package com.tgac.pldb;
import com.tgac.functional.Consumers;
import com.tgac.pldb.events.FactsChanged;
public interface Observer extends Consumers._2<FactsChanged, Database> {
}
