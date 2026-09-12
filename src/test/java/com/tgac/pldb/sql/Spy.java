package com.tgac.pldb.sql;

// ABOUTME: Opt-in statement logging for tests: -Dsql.spy reroutes a JDBC URL
// ABOUTME: through p6spy, printing every executed statement to stdout.

/**
 * The test suites' statement spy, off by default so test output stays
 * pristine. Run with {@code -Dsql.spy} and every JDBC statement — the
 * fetches, the flush inserts, the certify's lock and MAX probes, the
 * DDL — prints to stdout with parameters interpolated, via p6spy's
 * proxy driver ({@code spy.properties} holds the format). Wrap the URL
 * at its constant: {@code Spy.url("jdbc:h2:mem:...")}.
 */
public final class Spy {

	private Spy() {
	}

	public static String url(String jdbcUrl) {
		return Boolean.getBoolean("sql.spy")
				? "jdbc:p6spy:" + jdbcUrl.substring("jdbc:".length())
				: jdbcUrl;
	}
}
