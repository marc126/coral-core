package org.coral.core.web.grid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ognl.Ognl;

import org.coral.core.entity.BaseEntity;
import org.coral.core.mapper.JsonMapper;
import org.coral.core.utils.BeanUtils;
import org.coral.core.utils.UtilDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Page<T extends BaseEntity> implements Serializable {
	private static Logger logger = LoggerFactory.getLogger(Page.class);
	public Page(List<T> list,String[] rowNames,int page,long records, int size){
		this.entitys = list;
		this.rowNames=rowNames;
		this.page = page;
		this.records = records;
		this.size = size;
		for(T t:list)
			this.addOneRow(t, rowNames);
	}
	public Page(org.springframework.data.domain.Page<T> pg,String[] rowNames){
		this.entitys = pg.getContent();
		this.page = pg.getNumber();
		this.records = pg.getTotalElements();
		this.rowNames=rowNames;
		this.size = pg.getSize();
		for(T t:pg.getContent()){
			this.addOneRow(t, rowNames);
		}
	}
	//当前页面号
	int page;
	//总记录数
	long records;
	//列表内容
	List<Map<String,Object>> rows;
	//总页面数目
	int total;
	//每页大小
	int size;
	@JsonIgnore
	List<T> entitys;
	
	@JsonIgnore
	String[] rowNames;
	
	Map<String,Object> userdata;
	
	public void addOneRow(T obj,String[] rowNames){
		Map<String,Object> row = new HashMap<String,Object>();
		row.put("id", obj.getId());
		
		for(String n:rowNames){
			Object v = null;
			try {
				if(n.indexOf(".")>0){
					v = Ognl.getValue(n, obj);
				}else{
					v = BeanUtils.forceGetProperty(obj, n);
				}
				if(v instanceof java.util.Date){
					v = UtilDateTime.toDateString((java.util.Date) v,UtilDateTime.DEFAULTFORMAT);
				}
			} catch (Exception e) {
				//logger.error("对应属性列找不到"+obj+":"+n);
			}
			row.put(n, v);
		}

		this.getRows().add(row);
	}
	@Override
	public String toString(){
		JsonMapper j = new JsonMapper();
		return j.toJson(this);
	}

	/**
	 * @return 当前页码
	 */
	public int getPage() {
		return page;
	}

	/**
	 * @return 总记录数
	 */
	public Long getRecords() {
		return records;
	}

	public List<Map<String, Object>> getRows() {
		if(rows == null)
			rows = new ArrayList<Map<String, Object>>();
		return rows;
	}

	/**
	 * @return 每页条数
	 */
	public int getSize() {
		return size;
	}

	public int getTotal() {
		if (getRecords() % getSize() == 0)
			return getRecords().intValue()  / getSize() ;
		else
			return getRecords().intValue()   / getSize()  + 1;
	}
	
	public Map<String, Object> getUserdata() {
		return userdata;
	}
	
	public void setUserdata(Map<String, Object> userdata) {
		this.userdata = userdata;
	}
	
	public String[] getRowNames() {
		return rowNames;
	}
	public void setRowNames(String[] rowNames) {
		this.rows = new ArrayList<Map<String, Object>>();
		this.rowNames = rowNames;
		for(T t:entitys){
			this.addOneRow(t, this.rowNames);
		}
	}
	public List<T> getEntitys() {
		return entitys;
	}
	public void setEntitys(List<T> entitys) {
		this.rows = new ArrayList<Map<String, Object>>();
		this.entitys = entitys;
		for(T t:entitys){
			this.addOneRow(t, this.rowNames);
		}
	}
	
}
