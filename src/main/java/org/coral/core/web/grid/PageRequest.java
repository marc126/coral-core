package org.coral.core.web.grid;

import javax.servlet.http.HttpServletRequest;

import org.coral.core.entity.BaseEntity;
import org.coral.core.utils.UtilString;

public class PageRequest {
	
	public PageRequest(HttpServletRequest request,
			Class<? extends BaseEntity> entity, String[] fields) {

		String p = request.getParameter("page");
		if (UtilString.isNotEmpty(p))
			page = Integer.parseInt(p);
		else
			page = 1;

		String r = request.getParameter("rows");
		if (UtilString.isNotEmpty(r))
			rows = Integer.parseInt(r);
		else
			rows = 20;

		if (null != request.getAttribute("queryall") && (boolean)request.getAttribute("queryall")) {
			page = 1;
			rows = 999999;
		}

		sidx = request.getParameter("sidx");
		sord = request.getParameter("sord");
		String s = request.getParameter("_search");
		if ("true".equals(s))
			search = true;
		else
			search = false;

		this.entity = entity;
		this.searchField = request.getParameter("searchField");
		this.searchOper = request.getParameter("searchOper");
		this.searchString = request.getParameter("searchString");
		this.filters = request.getParameter("filters");
		this.fields = fields;
	}

	Integer rows;
	Integer page;
	String sidx;
	String sord;
	Boolean search;
	String searchField;
	String searchOper;
	String searchString;
	String filters;
	Class<? extends BaseEntity> entity;
	String[] fields;

	public String[] getFields() {
		return fields;
	}

	/**
	 * @return 每页条数
	 */
	public Integer getRows() {
		return rows;
	}

	/**
	 * @return 请求页面号
	 */
	public Integer getPage() {
		return page;
	}

	public String getSidx() {
		return sidx;
	}

	public String getSord() {
		return sord;
	}

	public Boolean isSearch() {
		return search;
	}

	public String getSearchField() {
		return searchField;
	}

	public String getSearchOper() {
		return searchOper;
	}

	public String getSearchString() {
		return searchString;
	}

	public String getFilters() {
		return filters;
	}

	public Boolean getSearch() {
		return search;
	}

	public void setSearch(Boolean search) {
		this.search = search;
	}

	public void setRows(Integer rows) {
		this.rows = rows;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public void setSidx(String sidx) {
		this.sidx = sidx;
	}

	public void setSord(String sord) {
		this.sord = sord;
	}

	public void setSearchField(String searchField) {
		this.searchField = searchField;
	}

	public void setSearchOper(String searchOper) {
		this.searchOper = searchOper;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public void setFilters(String filters) {
		this.filters = filters;
	}

	public void setFields(String[] fields) {
		this.fields = fields;
	}

	public Class<? extends BaseEntity> getEntity() {
		return entity;
	}

	public void setEntity(Class<? extends BaseEntity> entity) {
		this.entity = entity;
	}

}
