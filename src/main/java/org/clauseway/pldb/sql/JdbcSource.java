package org.clauseway.pldb.sql;

import java.sql.Connection;
import org.clauseway.pldb.AnswerSource;

public interface JdbcSource extends AnswerSource, AutoCloseable {
	Connection getConnection();

	/** The source's value↔wire table; builtins only unless overridden. */
	default Codecs codecs() {
		return Codecs.builtin();
	}
}
