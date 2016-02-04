package org.coral.core.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.TableGenerator;
@MappedSuperclass
public abstract class BaseIdEntity extends BaseEntity{
	
	@Id
	@TableGenerator(table="id_sequences",initialValue = 10000, allocationSize = 50, name = "ID_GENERATOR")
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "ID_GENERATOR")
	protected Long id;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
}
