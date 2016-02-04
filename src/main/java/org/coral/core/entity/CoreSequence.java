package org.coral.core.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "CORE_SEQUENCE")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class CoreSequence extends BaseUuidEntity {

	private static final long serialVersionUID = 971005874182682649L;
	
	@Column(length = 500, name = "MYKEY")
	String mykey;
	@Column(name = "MYVALUE")
	Long myvalue;
	public String getMykey() {
		return mykey;
	}
	public void setMykey(String mykey) {
		this.mykey = mykey;
	}
	public Long getMyvalue() {
		return myvalue;
	}
	public void setMyvalue(Long myvalue) {
		this.myvalue = myvalue;
	}

}
