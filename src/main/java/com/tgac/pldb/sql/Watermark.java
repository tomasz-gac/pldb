package com.tgac.pldb.sql;

// ABOUTME: The owned certify tier over standard SQL: per-relation marks in one
// ABOUTME: private table; commit = one short lock-compare-flush-advance transaction.

import com.tgac.logic.tabling.Call;
import com.tgac.logic.tabling.Condition;
import com.tgac.logic.unification.Reified;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.Certifiable;
import com.tgac.pldb.Footprint;
import com.tgac.pldb.Pin;
import com.tgac.pldb.relations.Fact;
import com.tgac.pldb.relations.Relation;
import io.vavr.Tuple2;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Value;

/**
 * Equips a source with the OWNED certify in plain standard SQL: a
 * private {@code watermark(relation, mark)} table with one lock row.
 * {@link #pin()} reads the snapshot's marks through the snapshot
 * connection. {@link #commit} runs on a FRESH connection from the
 * supplier — a snapshot cannot see the current world, and certify is
 * exactly a question about the current world: lock the lock row
 * {@code FOR UPDATE} (serializing committers), compare the footprints'
 * relations against current marks, land the facts, bump the moved
 * relations, commit — one transaction, atomic. A relation the pin never
 * saw a row for cannot be verified once anything moved (a concurrent
 * first write is indistinguishable from none), so it conservatively
 * conflicts. {@link #schema} is the DDL door — run it before any
 * transaction opens.
 */
public final class Watermark implements AnswerSource, Certifiable {

	private static final String LOCK_ROW = "*";

	private final AnswerSource source;
	private final Connection snapshot;
	private final Supplier<Connection> commits;

	private Watermark(AnswerSource source, Connection snapshot, Supplier<Connection> commits) {
		this.source = source;
		this.snapshot = snapshot;
		this.commits = commits;
	}

	public static Watermark over(AnswerSource source, Connection snapshot, Supplier<Connection> commits) {
		return new Watermark(source, snapshot, commits);
	}

	/** The watermark table and its lock row; idempotent, admin-connection DDL. */
	public static void schema(Statement admin) throws SQLException {
		admin.execute("CREATE TABLE IF NOT EXISTS watermark("
				+ "relation VARCHAR(128) PRIMARY KEY, mark BIGINT NOT NULL)");
		try (ResultSet lockRow = admin.executeQuery(
				"SELECT 1 FROM watermark WHERE relation = '" + LOCK_ROW + "'")) {
			if (!lockRow.next()) {
				admin.execute("INSERT INTO watermark(relation, mark) VALUES ('" + LOCK_ROW + "', 0)");
			}
		}
	}

	@Value
	private static class Marks implements Pin {
		Map<String, Long> marks;
	}

	@Override
	public Pin pin() {
		Map<String, Long> marks = new HashMap<>();
		try (PreparedStatement read = snapshot.prepareStatement(
				"SELECT relation, mark FROM watermark");
				ResultSet rows = read.executeQuery()) {
			while (rows.next()) {
				marks.put(rows.getString(1), rows.getLong(2));
			}
		} catch (SQLException e) {
			throw new IllegalStateException(id() + ": could not read the watermark", e);
		}
		return new Marks(marks);
	}

	@Override
	public boolean commit(Pin pin, Iterable<Footprint> reads, List<Fact> flush) {
		Map<String, Long> pinned = ((Marks) pin).getMarks();
		try (Connection commit = commits.get()) {
			commit.setAutoCommit(false);
			try {
				Map<String, Long> current = lockAndReadMarks(commit);
				if (!covers(pinned, current, reads)) {
					commit.rollback();
					return false;
				}
				SqlFlush.over(commit).flush(flush);
				advance(commit, flush);
				commit.commit();
				return true;
			} catch (SQLException | RuntimeException e) {
				commit.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new IllegalStateException(id() + ": certify failed", e);
		}
	}

	/** The lock row first (the commit lock), then every mark, all current. */
	private static Map<String, Long> lockAndReadMarks(Connection commit) throws SQLException {
		Map<String, Long> current = new HashMap<>();
		try (PreparedStatement lock = commit.prepareStatement(
				"SELECT relation, mark FROM watermark WHERE relation = ? FOR UPDATE")) {
			lock.setString(1, LOCK_ROW);
			try (ResultSet row = lock.executeQuery()) {
				if (!row.next()) {
					throw new SQLException("watermark lock row missing — run Watermark.schema first");
				}
				current.put(LOCK_ROW, row.getLong(2));
			}
		}
		try (PreparedStatement read = commit.prepareStatement(
				"SELECT relation, mark FROM watermark");
				ResultSet rows = read.executeQuery()) {
			while (rows.next()) {
				current.put(rows.getString(1), rows.getLong(2));
			}
		}
		return current;
	}

	private static boolean covers(Map<String, Long> pinned, Map<String, Long> current,
			Iterable<Footprint> reads) {
		if (current.getOrDefault(LOCK_ROW, 0L).equals(pinned.getOrDefault(LOCK_ROW, 0L))) {
			return true;
		}
		for (Footprint read : reads) {
			if (read.isEverything()) {
				return false;
			}
			for (String relation : read.relationNames()) {
				Long pinMark = pinned.get(relation);
				Long currentMark = current.get(relation);
				if (pinMark == null || currentMark == null || !currentMark.equals(pinMark)) {
					return false;
				}
			}
		}
		return true;
	}

	private static void advance(Connection commit, List<Fact> flushed) throws SQLException {
		Set<String> moved = new LinkedHashSet<>();
		moved.add(LOCK_ROW);
		for (Fact fact : flushed) {
			moved.add(fact.getRelation().getName());
		}
		for (String relation : moved) {
			bump(commit, relation);
		}
	}

	private static void bump(Connection commit, String relation) throws SQLException {
		try (PreparedStatement update = commit.prepareStatement(
				"UPDATE watermark SET mark = mark + 1 WHERE relation = ?")) {
			update.setString(1, relation);
			if (update.executeUpdate() == 0) {
				try (PreparedStatement insert = commit.prepareStatement(
						"INSERT INTO watermark(relation, mark) VALUES (?, 1)")) {
					insert.setString(1, relation);
					insert.executeUpdate();
				}
			}
		}
	}

	@Override
	public Iterable<Tuple2<Reified<?>, Condition>> answers(Call<Relation> probe) {
		return source.answers(probe);
	}

	@Override
	public long estimate(Call<Relation> probe) {
		return source.estimate(probe);
	}

	@Override
	public String id() {
		return source.id();
	}
}
