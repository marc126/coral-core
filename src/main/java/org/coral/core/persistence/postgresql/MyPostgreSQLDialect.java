package org.coral.core.persistence.postgresql;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQL94Dialect;

public class MyPostgreSQLDialect extends PostgreSQL94Dialect {
	public MyPostgreSQLDialect() {
		super();
		this.registerColumnType( Types.JAVA_OBJECT, "jsonb" );
	}
}
