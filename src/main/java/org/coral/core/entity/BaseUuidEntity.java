package org.coral.core.entity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;
@MappedSuperclass
public abstract class BaseUuidEntity extends BaseEntity{
	
	@Id
	@Column(length=22)
	@GenericGenerator(name = "uuidGenerator", strategy = "org.coral.core.entity.Base64UuidGenerator")
    @GeneratedValue(generator = "uuidGenerator")
	protected String id;
	
	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
