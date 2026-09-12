package com.tgac.pldb.sql;

// ABOUTME: Region-grain simulated serialization: a monotone version column per
// ABOUTME: table, the region pin is MAX(version), append-only makes phantoms visible.

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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Simulated serialization at REGION grain: every table carries a
 * monotone {@code version} column (the backend's private surface —
 * relations never declare it, the fetch never selects it, the flush
 * stamps it), and a region's pin is {@code MAX(version)} over the rows
 * matching the probe — {@code null} for an empty region. The certify
 * re-evaluates the same MAX under the commit lock through the SAME
 * rendering the fetch reads with ({@link RegionSql}): reading narrowly
 * and proving narrowly are one spelling. Stamps come from the
 * {@code watermark} lock row ({@link Watermark#schema}), bumped per
 * commit, so versions are monotone across the whole source and an
 * INSERT into a pinned region always moves its MAX — phantoms are
 * visible, which is what row-grain optimistic locking alone cannot do.
 *
 * <p>SOUND FOR APPEND-ONLY TABLES ONLY: an in-place UPDATE that keeps
 * its version, or a DELETE, moves a region without moving its MAX —
 * the same trust boundary as every simulated kind. Every table this
 * kind writes must have the version column; a table without one
 * refuses loudly at its first read.
 */
@Slf4j
@Value
@AllArgsConstructor
public class VersionedWatermark implements JdbcSource, SimulatedSerialization {
	private static final String LOCK_ROW = "*";
	private static final String VERSION_COLUMN = "version";

	CachingSqlFetch source;
	Supplier<Connection> commits;

	public static VersionedWatermark over(CachingSqlFetch source, Supplier<Connection> commits) {
		return new VersionedWatermark(source, commits);
	}

	/** One region's MAX(version) as of its read; {@code null} = empty region. */
	@Value
	private static class RegionMax implements Pin {
		Long max;
	}

	/** Pin BEFORE rows — capture-before; on the live lane the ordering is load-bearing. */
	@Override
	public Pinned<Iterable<Answer>> read(Call<Relation> probe) {
		Pin pin = new RegionMax(maxVersion(source.getConnection(), probe));
		return Pinned.of(source.answers(probe), pin);
	}

	@Override
	public boolean commit(Footprint read, List<Literal> flush) {
		try (Connection commit = commits.get()) {
			commit.setAutoCommit(false);
			try {
				long stamp = lockMark(commit) + 1;
				log.debug("{}: commit lock taken, stamp {}", id(), stamp);
				for (Map.Entry<Call<Relation>, Pin> pinned : read.pins().entrySet()) {
					Long current = maxVersion(commit, pinned.getKey());
					if (!Objects.equals(current, ((RegionMax) pinned.getValue()).getMax())) {
						log.debug("{}: region {}{} moved — pinned {}, current {}", id(),
								pinned.getKey().getRelation().getName(), pinned.getKey().getArguments(),
								((RegionMax) pinned.getValue()).getMax(), current);
						commit.rollback();
						return false;
					}
				}
				SqlFlush.over(commit, source.codecs())
						.stamped(VERSION_COLUMN, stamp)
						.flush(flush);
				bumpLock(commit);
				commit.commit();
				return true;
			} catch (SQLException | RuntimeException e) {
				commit.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new IllegalStateException(id() + ": versioned serialization failed", e);
		}
	}

	private Long maxVersion(Connection connection, Call<Relation> probe) {
		RegionSql region = source.region(probe);
		String sql = "SELECT MAX(" + VERSION_COLUMN + ") FROM "
				+ probe.getRelation().getName() + region.whereClause();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			int index = 1;
			for (Object parameter : region.getParameters()) {
				statement.setObject(index++, parameter);
			}
			try (ResultSet row = statement.executeQuery()) {
				row.next();
				long value = row.getLong(1);
				Long max = row.wasNull() ? null : value;
				log.debug("{}: {} ← {} = {}", id(), sql, region.getParameters(), max);
				return max;
			}
		} catch (SQLException e) {
			throw new IllegalStateException(id() + ": could not read MAX(" + VERSION_COLUMN
					+ ") of " + probe.getRelation().getName()
					+ " — the versioned kind requires a '" + VERSION_COLUMN + "' column", e);
		}
	}

	/** The lock row FOR UPDATE — the commit lock and the stamp sequence. */
	private static long lockMark(Connection commit) throws SQLException {
		try (
				PreparedStatement lock = commit.prepareStatement(
						"SELECT mark FROM watermark WHERE relation = ? FOR UPDATE")
		) {
			lock.setString(1, LOCK_ROW);
			try (ResultSet row = lock.executeQuery()) {
				if (!row.next()) {
					throw new SQLException("watermark lock row missing — run Watermark.schema first");
				}
				return row.getLong(1);
			}
		}
	}

	private static void bumpLock(Connection commit) throws SQLException {
		try (
				PreparedStatement update = commit.prepareStatement(
						"UPDATE watermark SET mark = mark + 1 WHERE relation = ?")
		) {
			update.setString(1, LOCK_ROW);
			update.executeUpdate();
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

	@Override
	public void close() throws Exception {
		source.close();
	}
}
