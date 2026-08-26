package com.tgac.pldb.sql;

// ABOUTME: The subsumptive cache half of the source: the landed pool plus the
// ABOUTME: coverage ledger — containment proof in, rows out, no backend knowledge.

import com.tgac.logic.tabling.Residues;
import com.tgac.pldb.Database;
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
import java.util.stream.Collectors;
import lombok.Value;

/**
 * Subsumptive reuse over landed fetches, knowing nothing of any backend:
 * fetched rows land in an in-memory database (idempotently — facts are
 * values), and the ledger records each fetch as (pattern, consumed region)
 * — the region its WHERE actually enforced. A probe is served locally only
 * on PROOF: its pattern subsumed by a covered one AND its full region
 * entailing the consumed one ({@code Residues.leq} true = containment
 * proven); anything short of proof re-fetches, idempotently. This is call
 * subsumption at the data boundary — wide serves narrow, never the
 * reverse.
 */
final class Landing {

	private Database landed = ImmutableDatabase.empty();
	private final Map<Relation, List<Coverage>> covered = new HashMap<>();

	/** One completed fetch: the probe's pattern plus the region its WHERE enforced. */
	@Value
	private static class Coverage {
		IndexedSeq<Optional<Object>> pattern;
		Residues consumed;
	}

	boolean isEmpty() {
		return covered.isEmpty();
	}

	boolean covers(Relation relation, IndexedSeq<Optional<Object>> probe, Residues region) {
		return covered.getOrDefault(relation, Collections.emptyList())
				.stream()
				.anyMatch(prior -> subsumes(prior.getPattern(), probe)
						&& region.leq(prior.getConsumed()));
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

	void land(Relation relation, IndexedSeq<Optional<Object>> pattern, Residues consumed, List<Fact> rows) {
		Set<Fact> resident = new HashSet<>();
		for (Fact fact : landed.get(relation, allFree(relation))) {
			resident.add(fact);
		}
		List<Fact> fresh = rows.stream()
				.filter(row -> !resident.contains(row))
				.collect(Collectors.toList());
		if (!fresh.isEmpty()) {
			landed = landed.withFacts(fresh)
					.getOrElseThrow(e -> new IllegalStateException("could not land rows of " + relation.getName(), e));
		}
		covered.computeIfAbsent(relation, r -> new ArrayList<>())
				.add(new Coverage(pattern, consumed));
	}

	Iterable<Fact> serve(Relation relation, IndexedSeq<Optional<Object>> probe) {
		return landed.get(relation, probe);
	}

	long estimate(Relation relation, IndexedSeq<Optional<Object>> probe) {
		return landed.estimate(relation, probe);
	}

	private static IndexedSeq<Optional<Object>> allFree(Relation relation) {
		return Array.fill(relation.getArgs().length, Optional.empty());
	}
}
