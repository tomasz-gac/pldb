package com.tgac.pldb.relations;

// ABOUTME: Extracts the package's knowledge about a probe's argument terms as a
// ABOUTME: live Residues region — the name cut per family, no renaming.

import com.tgac.logic.constraints.store.Constraint;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.goals.Package;
import com.tgac.logic.goals.Packaged;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.Term;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The lookup-side region extraction: for each constraint family, the covered
 * half of the name cut at the probe's argument variables — atoms whose whole
 * watched surface lies within the args. Coupled atoms (an arg tied to a
 * non-arg variable) fall to the remainder and stay home; ground args carry
 * no names and select nothing. Terms stay LIVE — no renaming — so a source
 * can resolve them back to argument positions; every atom in the region
 * watches only arg names, by construction of the cut.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Regions {

	public static Residues about(Package pkg, Array<? extends Term<?>> args) {
		List<LVar<?>> vars = new ArrayList<>();
		for (Term<?> arg : args) {
			Term<?> walked = pkg.walk(arg);
			if (walked.asVar().isDefined()) {
				vars.add(walked.asVar().get());
			}
		}
		Map<Class<?>, Theory<?>> covered = HashMap.empty();
		for (Packaged store : pkg.getStores().values()) {
			if (!(store instanceof Constraint)) {
				continue;
			}
			Constraint<?> pair = (Constraint<?>) store;
			Theory<?> about = pair.getTheory().split(vars)._1;
			if (!about.isEmpty()) {
				covered = covered.put(pair.getFactor().getClass(), about);
			}
		}
		return Residues.of(covered);
	}
}
