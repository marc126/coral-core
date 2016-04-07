package org.coral.core.entity;

import java.io.Serializable;

import org.coral.core.utils.UuidUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

public class Base64UuidGenerator implements IdentifierGenerator {

	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		return UuidUtils.compressedUuid();
	}

}
