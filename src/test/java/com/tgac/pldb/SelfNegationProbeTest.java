package com.tgac.pldb;

import static com.tgac.logic.goals.Goal.defer;
import static com.tgac.logic.nogoods.Exclusion.exclude;
import static com.tgac.logic.unification.LVar.lvar;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.relations.Literal;
import java.util.stream.Collectors;
import org.junit.Test;

public class SelfNegationProbeTest {

      /** p(x) :- x=1, NOT p(x) — the recursion deferred, so the engine meets it. */
      private static Literal selfNeg(Unifiable<Integer> x) {
              return Literal.relation("selfNeg")
                              .arg("x", x)
                              .solving(x.unifies(1).and(defer(() -> exclude(selfNeg(x)))));
      }

      @Test(timeout = 8000)
      public void deferredSelfNegationObserved() {
              Unifiable<Integer> x = lvar();
              try {
                      Object result = x.unifies(1).and(selfNeg(x)).solve(x).collect(Collectors.toList());
                      System.err.println("OUTCOME: completed with " + result);
              } catch (Throwable e) {
                      System.err.println("OUTCOME CLASS: " + e.getClass().getName());
                      String m = String.valueOf(e.getMessage());
                      System.err.println("OUTCOME MSG: " + m.substring(0, Math.min(300, m.length())));
              }
      }
}