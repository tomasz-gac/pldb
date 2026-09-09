package com.tgac.pldb.sql;

import com.tgac.pldb.AnswerSource;
import java.sql.Connection;

public interface JdbcSource extends AnswerSource, AutoCloseable {
	Connection getConnection();
}
