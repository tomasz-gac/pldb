package com.tgac.pldb.transaction;

// ABOUTME: The conflict predicate seam: a recognized commit failure maps to
// ABOUTME: Conflict, an unrecognized one surfaces as itself.

import static com.tgac.logic.unification.LVal.lval;
import static org.assertj.core.api.Assertions.assertThat;

import com.tgac.logic.unification.Unifiable;
import com.tgac.pldb.sql.Spy;
import com.tgac.pldb.AnswerSource;
import com.tgac.pldb.relations.Literal;
import com.tgac.pldb.sql.SerializableSource;
import io.vavr.control.Try;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TransactionPredicateTest {

	private Connection connection;

	@Before
	public void openDatabase() throws SQLException {
		connection = DriverManager.getConnection(Spy.url("jdbc:h2:mem:"));
	}

	@After
	public void closeDatabase() throws SQLException {
		connection.close();
	}

	private static Literal orphan(AnswerSource db, Unifiable<Integer> id) {
		return Literal.relation("orphan").arg("id", id).from(db);
	}

	/** No orphan table exists, so the flush inside commit fails for real. */
	private Try<?> commitFailure(Transaction db) {
		return db.withFacts(Collections.singletonList(orphan(null, lval(1))))
				.get().commit();
	}

	@Test
	public void aRecognizedCommitFailureMapsToConflict() {
		Try<?> refused = commitFailure(AbstractTransaction.over(SerializableSource.pinned("h2", connection, e -> true)));
		assertThat(refused.isFailure()).isTrue();
		assertThat(refused.getCause()).isInstanceOf(Transaction.Conflict.class);
	}

	@Test
	public void anUnrecognizedFailureSurfacesAsItself() {
		Try<?> refused = commitFailure(AbstractTransaction.over(SerializableSource.pinned("h2", connection, e -> false)));
		assertThat(refused.isFailure()).isTrue();
		assertThat(refused.getCause()).isNotInstanceOf(Transaction.Conflict.class);
	}
}
