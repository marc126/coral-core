package org.coral.core.web;

import org.coral.core.mapper.JsonMapper;
import org.springframework.beans.factory.FactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperFactoryBean implements FactoryBean<ObjectMapper> {

	@Override
	public ObjectMapper getObject() throws Exception {
		JsonMapper jm = new JsonMapper();
		return  jm.getMapper();
	}

	@Override
	public Class<?> getObjectType() {
		return ObjectMapper.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
