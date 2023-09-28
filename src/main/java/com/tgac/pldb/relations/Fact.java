package com.tgac.pldb.relations;

import io.vavr.collection.Array;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
@Value
@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PUBLIC)
public class Fact {
	Relation relation;
	Array<?> values;
}