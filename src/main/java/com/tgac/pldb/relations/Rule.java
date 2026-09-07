package com.tgac.pldb.relations;

// ABOUTME: A derived relation as a value: relation, heads, body. The goal reading
// ABOUTME: is the tabled call in the solve's table; the posting reads privately.

import static com.tgac.logic.unification.LVal.lval;

import com.tgac.functional.category.Nothing;
import com.tgac.functional.monad.Cont;
import com.tgac.logic.constraints.Postable;
import com.tgac.logic.constraints.Posting;
import com.tgac.logic.goals.Goal;
import com.tgac.logic.goals.Package;
import com.tgac.logic.goals.optimizer.Bounded;
import com.tgac.logic.tabling.Table;
import com.tgac.logic.tabling.Tabling;
import com.tgac.logic.unification.Substitutions;
import com.tgac.logic.unification.Term;
import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.GoalProducer;
import com.tgac.pldb.constraints.TableConstraints;
import io.vavr.collection.Array;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * A rule applied to arguments — the derived sibling of {@link Literal},
 * split because the two are different species: a literal READS a backend,
 * a rule IS a body over its heads. Bare in a conjunction it is an ordinary
 * tabled call in the SOLVE's table (Tabling.call keyed by the relation
 * value — every mint of one definition names the same entries, recursion
 * is the method calling itself and rings seal). Under {@code exclude}, or
 * explicitly {@link #posted()}, the constraint side reads the extension
 * through a {@link GoalProducer} over a per-posting private table: the
 * memo and the world it memoizes share one closure. Identity is by value
 * (name plus columns); one definition per name per solve is the caller's
 * discipline.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.MODULE)
public class Rule implements Goal, Bounded, Postable {
	Relation rel;
	Array<Unifiable<?>> heads;
	Goal body;

	@Override
	public Cont<Package, Nothing> apply(Package s) {
		requireGroundColumns(s);
		return Tabling.call(rel, heads.map(Unifiable::getObjectUnifiable), () -> body).apply(s);
	}

	/** The imposition reading — under {@code exclude} this is the only one. */
	@Override
	public Posting posted() {
		return TableConstraints.posted(
				GoalProducer.of(rel, body, heads, Table.empty()), rel, heads);
	}

	/** An open entry prices the barrier; the tabled call re-prices inside. */
	@Override
	public long answers(Substitutions s) {
		return Long.MAX_VALUE;
	}

	/** A ground-marked column is an input: free at application is a caller error. */
	private void requireGroundColumns(Package s) {
		Property<?>[] cols = rel.getArgs();
		for (int i = 0; i < cols.length; i++) {
			if (cols[i].isGround() && !((Term<?>) s.walk(heads.get(i))).asVal().isDefined()) {
				throw new IllegalStateException(rel.getName()
						+ ": ground column '" + cols[i].getName() + "' is unbound at application");
			}
		}
	}

	@Override
	public String toString() {
		return rel.toString() + heads;
	}
}
