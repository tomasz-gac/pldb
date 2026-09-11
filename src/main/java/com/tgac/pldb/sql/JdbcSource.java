package com.tgac.pldb.sql;

import com.tgac.pldb.AnswerSource;
import java.sql.Connection;

public interface JdbcSource extends AnswerSource, AutoCloseable {
	Connection getConnection();

	/** The source's value↔wire table; builtins only unless overridden. */
	default Codecs codecs() {
		return Codecs.builtin();
	}
}
