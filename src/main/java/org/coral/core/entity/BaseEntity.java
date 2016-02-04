package org.coral.core.entity;

import java.io.Serializable;

import javax.persistence.MappedSuperclass;
@MappedSuperclass
public abstract class BaseEntity implements Serializable{
	public abstract Serializable getId();
}
