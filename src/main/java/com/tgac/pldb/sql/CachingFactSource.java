package com.tgac.pldb.sql;

// ABOUTME: A FactSource that caches its delegate subsumptively: fetches land in a
// ABOUTME: pool, the ledger records (pattern, region), containment proof serves locally.

import com.tgac.logic.tabling.Residues;
import com.tgac.pldb.Database;
import com.tgac.pldb.FactSource;
import com.tgac.pldb.ImmutableDatabase;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Value;

/**
 * Subsumptive reuse over any delegate source — call subsumption at the data
 * boundary: wide serves narrow, never the reverse. Fetched rows land in an
 * in-memory pool (idempotently — facts are values), and the ledger records
 * each fetch as (pattern, region). Soundness rides the seam's own law: the
 * delegate must return every fact matching pattern ∧ region, so everything
 * in that region landed and the claim is honest REGARDLESS of how much of
 * the region the delegate actually enforced — which is why the recorded
 * region must be EXACTLY the one passed through, one object, one line. A
 * probe is served locally only on proof: its pattern subsumed by a covered
 * one and its full region entailing the recorded one ({@code Residues.leq}
 * true); anything short of proof re-fetches, idempotently.
 *
 * <p>Estimates delegate when uncovered, so a backend with real statistics
 * flows through; covered probes price exactly from the pool.
 */
public final class CachingFactSource implements FactSource {

	private final FactSource delegate;

	private Database landed = ImmutableDatabase.empty();
	private final Map<Relation, List<Coverage>> covered = new HashMap<>();

	/** One completed fetch: the probe's pattern plus the region passed with it. */
	@Value
	private static class Coverage {
		IndexedSeq<Optional<Object>> pattern;
		Residues region;
	}

	private CachingFactSource(FactSource delegate) {
		this.delegate = delegate;
	}

	public static CachingFactSource over(FactSource delegate) {
		return new CachingFactSource(delegate);
	}

	@Override
	public String id() {
		return delegate.id();
	}

	boolean isEmpty() {
		return covered.isEmpty();
	}

	@Override
	public Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args) {
		return get(relation, args, Residues.TRUE);
	}

	@Override
	public synchronized Iterable<Fact> get(Relation relation, IndexedSeq<Optional<Object>> args, Residues region) {
		if (!covers(relation, args, region)) {
			land(relation, delegate.get(relation, args, region));
			covered.computeIfAbsent(relation, r -> new ArrayList<>())
					.add(new Coverage(args, region));
		}
		return landed.get(relation, args);
	}

	@Override
	public synchronized long estimate(Relation relation, IndexedSeq<Optional<Object>> args) {
		return covers(relation, args, Residues.TRUE) ?
				landed.estimate(relation, args) :
				delegate.estimate(relation, args);
	}

	private boolean covers(Relation relation, IndexedSeq<Optional<Object>> probe, Residues region) {
		return covered.getOrDefault(relation, Collections.emptyList())
				.stream()
				.anyMatch(prior -> subsumes(prior.getPattern(), probe)
						&& region.leq(prior.getRegion()));
	}

	/** A wider probe subsumes a narrower one: its bound positions are a subset, values equal. */
	private static boolean subsumes(IndexedSeq<Optional<Object>> wide, IndexedSeq<Optional<Object>> narrow) {
		for (int i = 0; i < wide.size(); i++) {
			if (wide.get(i).isPresent()
					&& !wide.get(i).equals(narrow.get(i))) {
				return false;
			}
		}
		return true;
	}

	private void land(Relation relation, Iterable<Fact> rows) {
		Set<Fact> resident = new HashSet<>();
		for (Fact fact : landed.get(relation, allFree(relation))) {
			resident.add(fact);
		}
		List<Fact> fresh = new ArrayList<>();
		for (Fact row : rows) {
			if (!resident.contains(row)) {
				fresh.add(row);
			}
		}
		if (!fresh.isEmpty()) {
			landed = landed.withFacts(fresh)
					.getOrElseThrow(e -> new IllegalStateException("could not land rows of " + relation.getName(), e));
		}
	}

	private static IndexedSeq<Optional<Object>> allFree(Relation relation) {
		return Array.fill(relation.getArgs().length, Optional.empty());
	}

	@Override
	public String toString() {
		return "caching(" + delegate + ")";
	}
}
