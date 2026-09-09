package com.tgac.pldb.sql.transaction;

// ABOUTME: The regions a body of work read: EVERYTHING, or a set of probes —
// ABOUTME: the certify question's scope, never interpreted by its carrier.

import com.tgac.logic.tabling.Call;
import com.tgac.pldb.relations.Relation;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The read scope a certify question ranges over: {@link #EVERYTHING} when
 * the reads were not recorded (maximally conservative), or the set of
 * probe regions that were. A coarse {@link SimulatedSerialization} ignores it; a
 * finer one narrows its answer to the named relations or regions.
 */
public final class Footprint {

	public static final Footprint EVERYTHING = new Footprint(null);

	private final Set<Call<Relation>> regions;

	private Footprint(Set<Call<Relation>> regions) {
		this.regions = regions;
	}

	public static Footprint of(Collection<Call<Relation>> regions) {
		return new Footprint(Collections.unmodifiableSet(new LinkedHashSet<>(regions)));
	}

	public boolean isEverything() {
		return regions == null;
	}

	/** The probed regions; refuse on {@link #EVERYTHING} — check first. */
	public Set<Call<Relation>> regions() {
		if (regions == null) {
			throw new IllegalStateException("EVERYTHING has no region enumeration");
		}
		return regions;
	}

	/** The relation names the regions mention; refuse on {@link #EVERYTHING}. */
	public Set<String> relationNames() {
		return regions().stream()
				.map(region -> region.getRelation().getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Override
	public String toString() {
		return regions == null ? "EVERYTHING" : regions.toString();
	}
}
