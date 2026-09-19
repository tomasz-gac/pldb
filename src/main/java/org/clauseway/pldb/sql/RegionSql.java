package org.clauseway.pldb.sql;

// ABOUTME: A probe's region rendered as SQL: the WHERE conditions and their
// ABOUTME: parameters, in binding order — shared by the row fetch and the certify.

import java.util.List;
import lombok.Value;

/**
 * The SQL spelling of one probe's region: bound-column equalities,
 * null-bound IS NULLs, then the compiled residue predicates — conditions
 * and parameters in binding order. The row fetch SELECTs through it and
 * the versioned certify re-evaluates MAX(version) under it: reading
 * narrowly and proving narrowly are the same rendering.
 */
@Value
class RegionSql {
	List<String> conditions;
	List<Object> parameters;

	String whereClause() {
		return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
	}
}
