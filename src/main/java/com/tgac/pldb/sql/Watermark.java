package com.tgac.pldb.sql;

// ABOUTME: Simulated serialization over standard SQL: per-relation marks in one
// ABOUTME: private table; commit = one short lock-compare-flush-advance transaction.

import com.tgac.logic.tabling.Call;
import com.tgac.pldb.relations.Answer;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.relations.Relation;
import com.tgac.pldb.transaction.Footprint;
import com.tgac.pldb.transaction.Pin;
import com.tgac.pldb.transaction.Pinned;
import com.tgac.pldb.transaction.SimulatedSerialization;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Equips a source with SIMULATED serialization in plain standard SQL: a
 * private {@code watermark(relation, mark)} table with one lock row.
 * {@link #read} answers a probe WITH its region's mark, mark captured
 * first through the snapshot connection — the snapshot makes the
 * ordering moot. {@link #commit} runs on a FRESH connection from the
 * supplier — a snapshot cannot see the current world, and the proof is
 * exactly a question about the current world: lock the lock row
 * {@code FOR UPDATE} (serializing committers), compare every footprint
 * pin against the current marks, land the facts, bump the moved
 * relations, commit — one transaction, atomic. A mark row is minted by
 * a relation's first write, so a relation absent on BOTH sides of the
 * comparison is proven unmoved — double absence is silence, not
 * blindness. {@link #schema} is the DDL door — run it before any
 * transaction opens.
 */
@Value
@AllArgsConstructor
public class Watermark implements JdbcSource, SimulatedSerialization {
	private static final String LOCK_ROW = "*";

	JdbcSource source;
	Supplier<Connection> commits;

	public static Watermark over(JdbcSource source, Supplier<Connection> commits) {
		return new Watermark(source, commits);
	}

	/** The watermark table and its lock row; idempotent, admin-connection DDL. */
	public static void schema(Statement admin) throws SQLException {
		admin.execute("CREATE TABLE IF NOT EXISTS watermark("
				+ "relation VARCHAR(128) PRIMARY KEY, mark BIGINT NOT NULL)");
		try (
				ResultSet lockRow = admin.executeQuery(
						"SELECT 1 FROM watermark WHERE relation = '" + LOCK_ROW + "'")
		) {
			if (!lockRow.next()) {
				admin.execute("INSERT INTO watermark(relation, mark) VALUES ('" + LOCK_ROW + "', 0)");
			}
		}
	}

	@Override
	public Connection getConnection() {
		return source.getConnection();
	}

	@Override
	public Codecs codecs() {
		return source.codecs();
	}

	@Override
	public void close() throws Exception {
		source.close();
	}

	/** One region's mark as of its first read; {@code null} = no row = never written. */
	@Value
	private static class Mark implements Pin {
		Long mark;
	}

	/** Mark BEFORE rows — capture-before, though a snapshot makes the order moot. */
	@Override
	public Pinned<Iterable<Answer>> read(Call<Relation> probe) {
		Pin mark = markOf(probe.getRelation().getName());
		return Pinned.of(source.answers(probe), mark);
	}

	private Pin markOf(String relation) {
		try (
				PreparedStatement read = source.getConnection().prepareStatement(
						"SELECT mark FROM watermark WHERE relation = ?")
		) {
			read.setString(1, relation);
			try (ResultSet row = read.executeQuery()) {
				return new Mark(row.next() ? row.getLong(1) : null);
			}
		} catch (SQLException e) {
			throw new IllegalStateException(id() + ": could not read the watermark", e);
		}
	}

	@Override
	public boolean commit(Footprint read, List<Literal> flush) {
		try (Connection commit = commits.get()) {
			commit.setAutoCommit(false);
			try {
				Map<String, Long> current = lockAndReadMarks(commit);
				if (!covers(current, read)) {
					commit.rollback();
					return false;
				}
				SqlFlush.over(commit, source.codecs()).flush(flush);
				advance(commit, flush);
				commit.commit();
				return true;
			} catch (SQLException | RuntimeException e) {
				commit.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new IllegalStateException(id() + ": simulated serialization failed", e);
		}
	}

	/** The lock row first (the commit lock), then every mark, all current. */
	private static Map<String, Long> lockAndReadMarks(Connection commit) throws SQLException {
		Map<String, Long> current = new HashMap<>();
		try (
				PreparedStatement lock = commit.prepareStatement(
						"SELECT relation, mark FROM watermark WHERE relation = ? FOR UPDATE")
		) {
			lock.setString(1, LOCK_ROW);
			try (ResultSet row = lock.executeQuery()) {
				if (!row.next()) {
					throw new SQLException("watermark lock row missing — run Watermark.schema first");
				}
				current.put(LOCK_ROW, row.getLong(2));
			}
		}
		try (
				PreparedStatement read = commit.prepareStatement(
						"SELECT relation, mark FROM watermark");
				ResultSet rows = read.executeQuery()
		) {
			while (rows.next()) {
				current.put(rows.getString(1), rows.getLong(2));
			}
		}
		return current;
	}

	private static boolean covers(Map<String, Long> current, Footprint read) {
		for (Map.Entry<Call<Relation>, Pin> pinned : read.pins().entrySet()) {
			String relation = pinned.getKey().getRelation().getName();
			if (!Objects.equals(current.get(relation), ((Mark) pinned.getValue()).getMark())) {
				return false;
			}
		}
		return true;
	}

	private static void advance(Connection commit, List<Literal> flushed) throws SQLException {
		Set<String> moved = new LinkedHashSet<>();
		moved.add(LOCK_ROW);
		for (Literal row : flushed) {
			moved.add(row.getRel().getName());
		}
		for (String relation : moved) {
			bump(commit, relation);
		}
	}

	private static void bump(Connection commit, String relation) throws SQLException {
		try (
				PreparedStatement update = commit.prepareStatement(
						"UPDATE watermark SET mark = mark + 1 WHERE relation = ?")
		) {
			update.setString(1, relation);
			if (update.executeUpdate() == 0) {
				try (
						PreparedStatement insert = commit.prepareStatement(
								"INSERT INTO watermark(relation, mark) VALUES (?, 1)")
				) {
					insert.setString(1, relation);
					insert.executeUpdate();
				}
			}
		}
	}

	@Override
	public Iterable<Answer> answers(Call<Relation> probe) {
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
