package com.tgac.pldb.relations;

// ABOUTME: SQL NULL as a VALUE: the sentinel a nullable column's null cell reads
// ABOUTME: as — it equals itself and nothing else, restoring two-valued logic.

/**
 * The null sentinel. A nullable column's NULL cell becomes this value —
 * not a free variable (SQL's NULL joins with NOTHING; a free variable
 * joins with everything) and not an absence (the row exists). As an
 * ordinary value it unifies with itself only, is a member of no domain,
 * and satisfies no comparison — which keeps engine answers consistent
 * with what a pushed WHERE clause delivers.
 */
public final class Null {

	public static final Null VALUE = new Null();

	private Null() {
	}

	@Override
	public String toString() {
		return "NULL";
	}
}
