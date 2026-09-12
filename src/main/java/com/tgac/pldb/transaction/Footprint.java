package com.tgac.pldb.transaction;

// ABOUTME: The regions a body of work read, each with the pin captured at its
// ABOUTME: first touch — the certify question's scope, never interpreted by its carrier.

import com.tgac.logic.tabling.Call;
import com.tgac.pldb.relations.Relation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The read scope a certify question ranges over: every probed region,
 * paired with the {@link Pin} the source minted when that region was
 * first read. The carrier never interprets the pins — the source
 * answers every question about them at commit. An empty footprint
 * certifies vacuously: a decision that stood on no reads cannot have
 * stood on stale ones.
 */
public final class Footprint {

	private final Map<Call<Relation>, Pin> pins;

	private Footprint(Map<Call<Relation>, Pin> pins) {
		this.pins = pins;
	}

	public static Footprint of(Map<Call<Relation>, Pin> pins) {
		return new Footprint(Collections.unmodifiableMap(new LinkedHashMap<>(pins)));
	}

	/** Each probed region with the pin captured at its first touch. */
	public Map<Call<Relation>, Pin> pins() {
		return pins;
	}

	@Override
	public String toString() {
		return pins.keySet().toString();
	}
}
