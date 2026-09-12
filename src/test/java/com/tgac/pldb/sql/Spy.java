package com.tgac.pldb.sql;

// ABOUTME: Statement logging for tests, ON by default: JDBC URLs route through
// ABOUTME: p6spy, printing every executed statement to stdout; -Dsql.spy=false mutes.

/**
 * The test suites' statement spy, ON by default: every JDBC statement —
 * the fetches, the flush inserts, the certify's lock and MAX probes,
 * the DDL — prints to stdout with parameters interpolated, via p6spy's
 * proxy driver ({@code spy.properties} holds the format). Run with
 * {@code -Dsql.spy=false} to mute. Wrap the URL at its constant:
 * {@code Spy.url("jdbc:h2:mem:...")}.
 */
public final class Spy {

	private Spy() {
	}

	public static String url(String jdbcUrl) {
		return "false".equals(System.getProperty("sql.spy"))
				? jdbcUrl
				: "jdbc:p6spy:" + jdbcUrl.substring("jdbc:".length());
	}
}
