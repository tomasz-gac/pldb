package com.tgac.pldb.relations;

// ABOUTME: Extracts the package's knowledge about a probe's argument terms as a
// ABOUTME: Residues region — the name cut per family, renamed to POSITIONAL names.

import com.tgac.logic.constraints.store.Constraint;
import com.tgac.logic.constraints.store.Renaming;
import com.tgac.logic.constraints.store.Theory;
import com.tgac.logic.goals.Package;
import com.tgac.logic.goals.Packaged;
import com.tgac.logic.tabling.Residues;
import com.tgac.logic.unification.Any;
import com.tgac.logic.unification.LVar;
import com.tgac.logic.unification.Name;
import com.tgac.logic.unification.Term;
import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The lookup-side region extraction: for each constraint family, the covered
 * half of the name cut at the probe's argument variables — atoms whose whole
 * watched surface lies within the args. Coupled atoms (an arg tied to a
 * non-arg variable) fall to the remainder and stay home; ground args carry
 * no names and select nothing.
 *
 * <p>The extracted atoms are renamed to POSITIONAL canonical names: the
 * variable at argument position i becomes {@code Any.of(i)} (first position
 * wins when one variable fills several). Two things ride on this: a source
 * resolves {@code _.i} straight to its i-th column with no term traffic
 * through the seam, and regions from different probes — different solves,
 * different live variables — stay comparable, which is what coverage
 * containment reads.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Regions {

	public static Residues about(Package pkg, Array<? extends Term<?>> args) {
		List<LVar<?>> vars = new ArrayList<>();
		java.util.Map<Name<?>, Term<?>> positional = new LinkedHashMap<>();
		for (int i = 0; i < args.size(); i++) {
			Term<?> walked = pkg.walk(args.get(i));
			if (walked.asVar().isDefined()) {
				LVar<?> var = walked.asVar().get();
				vars.add(var);
				positional.putIfAbsent(var, Any.of(i));
			}
		}
		Renaming canonical = Renaming.of(positional);
		Map<Class<?>, Theory<?>> covered = HashMap.empty();
		for (Packaged store : pkg.getStores().values()) {
			if (!(store instanceof Constraint)) {
				continue;
			}
			Constraint<?> pair = (Constraint<?>) store;
			Theory<?> about = pair.getTheory().split(vars)._1;
			if (!about.isEmpty()) {
				covered = covered.put(pair.getFactor().getClass(),
						(Theory<?>) about.rename(canonical).ground());
			}
		}
		return Residues.of(covered);
	}
}
