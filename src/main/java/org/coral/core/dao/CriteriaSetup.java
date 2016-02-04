package org.coral.core.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Restrictions;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.CriteriaImpl.Subcriteria;

/**
 * 查询回调类,组织查询条件
 * 
 */
public class CriteriaSetup {

	public enum MatchType {
		EQ, LIKE, LT, GT, LE, GE, ILIKE, NE, BEGIN, END, NULL, NNULL, NLIKE, NBEGIN, NEND, IN, NIN;
	}

	public enum OrderType {
		ASC, DESC;
	}

	/**
	 * 由业务传入多个查询条件 由Restrictions类实现查询条件封装到List对象中
	 */
	private List<Criterion> criterions = new ArrayList<Criterion>();
	Projection projection;
	/** A List<Order> variable :排序条件 */
	private Map<String, OrderType> sortMap = new LinkedHashMap<String, OrderType>();

	/** 简单的过虑条件 */
	private List<PropertyFilter> filters = new ArrayList<PropertyFilter>();

	public class PropertyFilter {
		public String key;
		public MatchType matchType;
		public Object value;

		public PropertyFilter(String key, Object value) {
			this.key = key;
			this.value = value;
			this.matchType = MatchType.EQ;
		}

		public PropertyFilter(String key, Object value, MatchType matchType) {
			this.key = key;
			this.value = value;
			this.matchType = matchType;
		}
	}

	/**
	 * 使用Criteria建立关联查询时,必须使用createAlias 此处定义的是自定义关联的别名
	 **/
	private Map<String, String> aliases = new HashMap<String, String>();

	public static boolean isExistAlias(Criteria criteria, String alias) {
		boolean existing = false;
		CriteriaImpl impl = (CriteriaImpl) criteria;
		Iterator iterator = impl.iterateSubcriteria();
		for (; iterator.hasNext();) {
			Subcriteria subcriteria = (Subcriteria) iterator.next();
			if (subcriteria.getPath().equals(alias)) {
				existing = StringUtils.isNotEmpty(subcriteria.getAlias());
				break;
			}
		}
		return existing;
	}

	public boolean isExistAlias(String alias) {
		return aliases.containsKey(alias);
	}

	public void setup(Criteria criteria) {
		// 创建自定义关联的别名
		if (aliases != null && !aliases.isEmpty()) {
			Set<String> keys = aliases.keySet();
			for (String associationPath : keys) {
				String alias = aliases.get(associationPath);
				if (!isExistAlias(criteria, associationPath)) {
					criteria.createAlias(associationPath, alias);
				}
			}
		}
		
		// 得到简单条件
		if (filters != null && !filters.isEmpty()) {
			for (PropertyFilter filter : filters) {
				String fieldName = makeAlias(criteria, filter.key);
				switch (filter.matchType) {
				case EQ:
					criteria.add(Restrictions.eq(fieldName, filter.value));
					break;
				case ILIKE:
					criteria.add(Restrictions.ilike(fieldName,
							filter.value.toString(), MatchMode.ANYWHERE));
					break;
				case LIKE:
					criteria.add(Restrictions.like(fieldName,
							filter.value.toString(), MatchMode.ANYWHERE));
					break;
				case LT:
					criteria.add(Restrictions.lt(fieldName, filter.value));
					break;
				case GT:
					criteria.add(Restrictions.gt(fieldName, filter.value));
					break;
				case LE:
					criteria.add(Restrictions.le(fieldName, filter.value));
					break;
				case GE:
					criteria.add(Restrictions.ge(fieldName, filter.value));
					break;
				case NE:
					criteria.add(Restrictions.ne(fieldName, filter.value));
					break;
				case BEGIN:
					criteria.add(Restrictions.like(fieldName,
							filter.value.toString(), MatchMode.START));
					break;
				case END:
					criteria.add(Restrictions.like(fieldName,
							filter.value.toString(), MatchMode.END));
					break;
				case NULL:
					criteria.add(Restrictions.isNull(fieldName));
					break;
				case NNULL:
					criteria.add(Restrictions.isNotNull(fieldName));
					break;
				case NLIKE:
					criteria.add(Restrictions.not(Restrictions.like(fieldName,
							filter.value.toString(), MatchMode.ANYWHERE)));
					break;
				case NBEGIN:
					criteria.add(Restrictions.not(Restrictions.like(fieldName,
							filter.value.toString(), MatchMode.START)));
					break;
				case NEND:
					criteria.add(Restrictions.not(Restrictions.like(fieldName,
							filter.value.toString(), MatchMode.END)));
					break;
				case IN:
					criteria.add(Restrictions.in(fieldName,
							(Object[]) filter.value));
					break;
				case NIN:
					criteria.add(Restrictions.not(Restrictions.in(fieldName,
							(Object[]) filter.value)));
					break;
				}

			}
		}

		// 多个查询条件
		if (criterions != null && criterions.size() > 0) {
			for (Criterion criter : criterions) {
				criteria.add(criter);
			}
		}

		// 多个排序条件
		if (!sortMap.isEmpty()) {
			for (Object o : sortMap.keySet()) {
				String fieldName = o.toString();
				OrderType orderType = sortMap.get(fieldName);

				// 处理嵌套属性如category.name,a.b.c
				fieldName = makeAlias(criteria, fieldName);

				switch (orderType) {
				case ASC:
					criteria.addOrder(Order.asc(fieldName));
					break;
				case DESC:
					criteria.addOrder(Order.desc(fieldName));
					break;
				}
			}
		}
		
		if(projection!=null)
			criteria.setProjection(projection);
	}

	private String makeAlias(Criteria c, String name) {
		String fieldName = name;
		if (name.indexOf('.') != -1) {
			String lastNode = StringUtils.substringAfterLast(name, ".");
			String[] as = name.split("\\.");
			String alias = "";
			for (int i = 0; i < as.length - 1; i++) {
				if ("".equals(alias))
					alias += as[i];
				else
					alias += "." + as[i];
				if (!isExistAlias(c, alias))
					c.createAlias(alias, as[i]);
				fieldName = as[i] + "." + lastNode;
				;
			}
		}
		return fieldName;
	}

	public void clearCriterion() {
		this.criterions.clear();
	}

	public void clearAlias() {
		this.aliases.clear();
	}

	public void clearSort() {
		this.sortMap.clear();
	}

	public void clearFilter() {
		this.filters.clear();
	}

	public void addCriterion(Criterion criterion) {
		this.criterions.add(criterion);
	}

	public void addAlias(String associationPath, String alias) {
		this.aliases.put(associationPath, alias);
	}

	public void addSort(String field, OrderType orderType) {
		this.sortMap.put(field, orderType);
	}

	public void addFilter(String key, Object value) {
		this.filters.add(new PropertyFilter(key, value));
	}

	public void addFilter(String key, Object value, MatchType matchType) {
		this.filters.add(new PropertyFilter(key, value, matchType));
	}

	public List<Criterion> getCriterions() {
		return criterions;
	}

	public void setCriterions(List<Criterion> criterions) {
		this.criterions = criterions;
	}

	public Map<String, OrderType> getSortMap() {
		return sortMap;
	}

	public void setSortMap(Map<String, OrderType> sortMap) {
		this.sortMap = sortMap;
	}

	public List<PropertyFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<PropertyFilter> filters) {
		this.filters = filters;
	}

	public Map<String, String> getAliases() {
		return aliases;
	}

	public void setAliases(Map<String, String> aliases) {
		this.aliases = aliases;
	}

	public Projection getProjection() {
		return projection;
	}

	public void setProjection(Projection projection) {
		this.projection = projection;
	}

}
