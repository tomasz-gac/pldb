package com.tgac.pldb;
import com.tgac.functional.Consumers;
import com.tgac.pldb.events.ChangeType;
import com.tgac.pldb.events.FactsChanged;
import com.tgac.pldb.relations.Relation;

import java.util.stream.Collectors;
public interface Observer extends Consumers._2<FactsChanged, Database> {
	static Observer ofRelation(Relation rel, Observer c) {
		return (fc, db) -> c.accept(
				FactsChanged.of(fc.getChange(), fc.getFacts().stream()
						.filter(f -> rel.equals(f.getRelation()))
						.collect(Collectors.toList())),
				db);
	}

	static Observer ofChange(ChangeType changeType, Observer c) {
		return (fc, db) -> {
			if(fc.getChange() == changeType){
				c.accept(fc, db);
			}
		};
	}
}
