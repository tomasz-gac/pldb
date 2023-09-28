package com.tgac.pldb;
import com.tgac.logic.Unifiable;
import com.tgac.pldb.events.DatabaseEventListener;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;

import java.util.List;
public interface Database {
	Iterable<IndexedSeq<Object>> get(Relation relation, IndexedSeq<Unifiable<?>> args);

	Validation<Seq<String>, Database> facts(List<Fact> facts);

	Validation<Seq<String>, Database> retract(List<Fact> facts);

	Database observe(DatabaseEventListener handler);
}
