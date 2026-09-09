package com.tgac.pldb.sql;

// ABOUTME: An AnswerSource that caches its delegate subsumptively: answers land in a
// ABOUTME: pool, the ledger records probes as calls, Call.subsumes proves coverage.

import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.inmemory.Database;
import com.tgac.pldb.inmemory.ImmutableDatabase;
import com.tgac.pldb.relations.Answers;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Subsumptive reuse over any delegate source — call subsumption at the data
 * boundary: wide serves narrow, never the reverse. Fetched answers land in
 * an in-memory pool (idempotently — rows are values), and the ledger records
 * each fetch AS ITS CALL. Soundness rides the seam's own law: the delegate
 * must return every answer matching the probe, so everything in the probe's
 * region landed and the claim is honest REGARDLESS of how much of the region
 * the delegate actually enforced — which is why the recorded probe is
 * EXACTLY the call passed through, one object, one line. A probe is served
 * locally only on proof: a recorded call that {@link Call#subsumes} it —
 * relation identity, argument subsumption, residue entailment, the one
 * containment judgment the key stack already owns. Anything short of proof
 * re-fetches, idempotently.
 *
 * <p>Estimates delegate when uncovered, so a backend with real statistics
 * flows through; covered probes price exactly from the pool.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CachingAnswerSource implements AnswerSource {

	private final AnswerSource delegate;

	private Database cache = ImmutableDatabase.empty();
	private final Map<Relation, List<Call<Relation>>> covered = new HashMap<>();

	public static CachingAnswerSource over(AnswerSource delegate) {
		return new CachingAnswerSource(delegate);
	}

	@Override
	public String id() {
		return delegate.id();
	}

	boolean isEmpty() {
		return covered.isEmpty();
	}

	@Override
	public synchronized Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		if (!covers(probe)) {
			add(probe, delegate.answers(probe));
			covered.computeIfAbsent(probe.getRelation(), r -> new ArrayList<>())
					.add(probe);
		}
		return cache.answers(pattern(probe));
	}

	@Override
	public synchronized long estimate(Call<Relation> probe) {
		return covers(pattern(probe)) ?
				cache.estimate(probe) :
				delegate.estimate(probe);
	}

	/** The probe widened to its pattern: region dropped, the pool's serve key. */
	private static Call<Relation> pattern(Call<Relation> probe) {
		return Call.of(probe.getRelation(), probe.getArguments());
	}

	private boolean covers(Call<Relation> probe) {
		return covered.getOrDefault(probe.getRelation(), Collections.emptyList())
				.stream()
				.anyMatch(prior -> prior.subsumes(probe));
	}

	private void add(Call<Relation> probe, Iterable<Tuple2<Reified<?>, Condition>> answers) {
		// every incoming row matches the originating probe, so any resident
		// duplicate does too: the probe pattern's own (indexed) bucket is the
		// whole dedup universe
		Relation relation = probe.getRelation();
		Set<Tuple2<Reified<?>, Condition>> resident = new HashSet<>();
		for (Tuple2<Reified<?>, Condition> fact : cache.answers(probe)) {
			resident.add(fact);
		}
		List<Fact> fresh = new ArrayList<>();
		for (Tuple2<Reified<?>, Condition> answer : answers) {
			if (!Condition.ONE.equals(answer._2)) {
				// the pool is ground; a conditional answer cannot land without
				// dropping its condition — under-delivery — so refuse
				throw new IllegalStateException(
						"conditional answers cannot land in the ground pool: " + answer);
			}
			Fact row = Fact.of(relation, Answers.values(answer._1));
			if (!resident.contains(answer)) {
				fresh.add(row);
			}
		}
		if (!fresh.isEmpty()) {
			cache = cache.withFacts(fresh)
					.getOrElseThrow(e -> new IllegalStateException(
							"could not land rows of " + relation.getName(), e));
		}
	}

	@Override
	public String toString() {
		return "caching(" + delegate + ")";
	}
}
