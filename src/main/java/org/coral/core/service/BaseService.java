package org.coral.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.EntityType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coral.core.dao.QuerySetup;
import org.coral.core.entity.BaseEntity;
import org.coral.core.exception.Assert;
import org.coral.core.utils.GenericsUtils;
import org.coral.core.utils.UtilString;
import org.coral.core.web.grid.Page;
import org.coral.core.web.grid.PageRequest;
import org.coral.core.web.grid.QueryUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public abstract class BaseService<T extends BaseEntity> {
	protected Log logger = LogFactory.getLog(this.getClass());
	@PersistenceContext
	protected EntityManager em;
	private Class<T> entityClass;

	protected Class<T> getEntityClass() {
		if (entityClass == null)
			entityClass = GenericsUtils.getSuperClassGenricType(getClass(), 0);
		return entityClass;
	}

	public Long getCount(QuerySetup qs) {
		// 先把Projection和OrderBy条件取出来,清空两者来执行Count操作
		Selection selection = qs.getCriteriaQuery().getSelection();
		List<Order> orders = qs.getOrders();
		qs.getCriteriaQuery().select(qs.getCriteriaBuilder().count(qs.getFrom()));
		qs.setOrders(new ArrayList());
		Long count = (Long) em.createQuery(qs.newCriteriaQuery()).getSingleResult();
		qs.getCriteriaQuery().select(selection);
		qs.setOrders(orders);
		return count;
	}

	/**
	 * 分页查询
	 * 
	 * @param query
	 *            查询条件
	 * @param pageNo
	 *            页号
	 * @param rowsPerPage
	 *            每页显示条数
	 */
	public Page<T> queryPage(QuerySetup qs, int pageNo, int rowsPerPage) {
		if (pageNo <= 0)
			pageNo = 1;
		if (rowsPerPage <= 0)
			rowsPerPage = 20;
		logger.debug(qs.getClazz() + "-----开始查询,页码:" + pageNo + ",每页显示:" + rowsPerPage + "----");
		logger.debug("查询条件:");
		for (Predicate cri : qs.getPredicates())
			logger.debug(cri);

		int count = getCount(qs).intValue();

		// 当把最后一页数据删除以后,页码会停留在最后一个上必须减一
		int totalPageCount = count / rowsPerPage;
		if (pageNo > totalPageCount && (count % rowsPerPage == 0)) {
			pageNo = totalPageCount;
		}
		if (pageNo - totalPageCount > 2) {
			pageNo = totalPageCount + 1;
		}
		int firstResult = (pageNo - 1) * rowsPerPage;
		if (firstResult < 0) {
			firstResult = 0;
		}
		List<T> result = em.createQuery(qs.newCriteriaQuery()).setFirstResult(firstResult).setMaxResults(rowsPerPage)
				.getResultList();
		return new Page<T>(result, pageNo, count, rowsPerPage);
	}

	public Page<T> findPage(QuerySetup qs, PageRequest pr) {

		// 转换过滤条件
		QueryUtils.buildFilter(pr, qs);
		if (UtilString.isNotEmpty(pr.getSidx())) {
			if ("asc".equals(pr.getSord()))
				qs.setOrder(pr.getSidx(), "asc");
			else
				qs.setOrder(pr.getSidx(), "desc");
		}
		Page<T> page = queryPage(qs, pr.getPage(), pr.getRows());
		page.setRowNames(pr.getFields());
		return page;
	}

	public Page<T> findPage(PageRequest pr) {
		QuerySetup qs = QuerySetup.forClass(getEntityClass(), em);
		return findPage(qs, pr);
	}
	public QuerySetup getQuerySetup(){
		return QuerySetup.forClass(getEntityClass(), em);
	}
	
	
	/**
	 * 判断实体是否存在
	 * 
	 * @param entity
	 * @return
	 */
	public Boolean containsEntity(Object entity) {
		return em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(entity);
	}

	/**
	 * 取得对象的主键名.
	 */
	public String getIdName() {
		EntityType entityType = em.getMetamodel().entity(getEntityClass());
		return entityType.getId(entityType.getIdType().getJavaType()).getName();
	}

	/**
	 * 保存新增或修改的对象.
	 */
	@Transactional(readOnly = false)
	public void save(final T entity) {
		Assert.notNull(entity, "entity不能为空");
		em.persist(entity);
		logger.debug("save entity: " + entity.toString());
	}

	/**
	 * 删除对象.
	 * 
	 * @param entity
	 *            对象必须是session中的对象或含id属性的transient对象.
	 */
	@Transactional(readOnly = false)
	public void delete(final T entity) {
		Assert.notNull(entity, "entity不能为空");
		em.remove(entity);
		logger.debug("save entity: " + entity.toString());
	}

	/**
	 * 按id删除对象.
	 */
	@Transactional(readOnly = false)
	public void delete(final Serializable id) {
		Assert.notNull(id, "id不能为空");
		delete(get(id));
		logger.debug("delete entity " + getEntityClass().getSimpleName() + " ,id is " + id);
	}

	/**
	 * 批量删除
	 */
	@Transactional(readOnly = false)
	public void batchDelete(final Serializable[] ids) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Serializable id : ids) {
			if (id instanceof String)
				id = "'" + id + "'";
			sb.append(id + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		batchExecute("delete from " + getEntityClass().getSimpleName() + " where " + getIdName() + " in " + sb);
	}

	/**
	 * 按id获取对象.
	 */
	public T get(final Serializable id) {
		Assert.notNull(id, "id不能为空");
		return em.find(getEntityClass(), id);
	}

	/**
	 * 按id列表获取对象列表.
	 */
	public List<T> get(final Collection<Serializable> ids) {
		CriteriaQuery<T> criteriaQuery = em.getCriteriaBuilder().createQuery(getEntityClass());
		Root<T> r = criteriaQuery.from(getEntityClass());
		In in = em.getCriteriaBuilder().in(r.get(getIdName()));
		for (Serializable id : ids) {
			in.value(id);
		}
		criteriaQuery.where(in);
		return em.createQuery(criteriaQuery).getResultList();
	}

	/**
	 * 获取全部对象.
	 */
	public List<T> getAll() {
		CriteriaQuery<T> criteriaQuery = em.getCriteriaBuilder().createQuery(getEntityClass());
		Root<T> r = criteriaQuery.from(getEntityClass());
		return em.createQuery(criteriaQuery).getResultList();
	}

	/**
	 * 获取全部对象, 支持按属性行序.
	 */
	public List<T> getAll(String orderByProperty, boolean isAsc) {
		Assert.hasText(orderByProperty, "orderByProperty不能为空");
		CriteriaQuery<T> criteriaQuery = em.getCriteriaBuilder().createQuery(getEntityClass());

		Root<T> r = criteriaQuery.from(getEntityClass());
		Order order;
		if (isAsc) {
			order = em.getCriteriaBuilder().asc(r.get(orderByProperty));
		} else {
			order = em.getCriteriaBuilder().desc(r.get(orderByProperty));
		}
		criteriaQuery.orderBy(order);
		return em.createQuery(criteriaQuery).getResultList();
	}

	/**
	 * 按属性查找对象列表, 匹配方式为相等.
	 */
	public List<T> findBy(final String propertyName, final Object value) {
		Assert.hasText(propertyName, "propertyName不能为空");
		CriteriaQuery<T> criteriaQuery = em.getCriteriaBuilder().createQuery(getEntityClass());

		Root<T> r = criteriaQuery.from(getEntityClass());
		Predicate p = em.getCriteriaBuilder().equal(r.get(propertyName), value);
		criteriaQuery.where(p);
		return em.createQuery(criteriaQuery).getResultList();
	}

	/**
	 * 按属性查询,并按某个属性排序
	 */
	public List<T> findBy(final String propertyName, final Object value, String orderByProperty, boolean isAsc) {
		Assert.hasText(propertyName, "propertyName不能为空");
		CriteriaQuery<T> criteriaQuery = em.getCriteriaBuilder().createQuery(getEntityClass());

		Root<T> r = criteriaQuery.from(getEntityClass());
		Predicate p = em.getCriteriaBuilder().equal(r.get(propertyName), value);
		criteriaQuery.where(p);

		Order order;
		if (isAsc) {
			order = em.getCriteriaBuilder().asc(r.get(orderByProperty));
		} else {
			order = em.getCriteriaBuilder().desc(r.get(orderByProperty));
		}
		criteriaQuery.orderBy(order);
		return em.createQuery(criteriaQuery).getResultList();
	}

	/**
	 * 按属性查找唯一对象, 匹配方式为相等.
	 */
	public T findUniqueBy(final String propertyName, final Object value) {
		Assert.hasText(propertyName, "propertyName不能为空");
		CriteriaQuery<T> criteriaQuery = em.getCriteriaBuilder().createQuery(getEntityClass());

		Root<T> r = criteriaQuery.from(getEntityClass());
		Predicate p = em.getCriteriaBuilder().equal(r.get(propertyName), value);
		criteriaQuery.where(p);
		return em.createQuery(criteriaQuery).getSingleResult();
	}

	/**
	 * 按HQL查询对象列表.
	 * 
	 * @param values
	 *            数量可变的参数,按顺序绑定.
	 */
	public <X> List<X> find(final String hql, final Object... values) {
		return createQuery(hql, values).getResultList();
	}

	/**
	 * 按HQL查询对象列表.
	 * 
	 * @param values
	 *            命名参数,按名称绑定.
	 */
	public <X> List<X> find(final String hql, final Map<String, ?> values) {
		return createQuery(hql, values).getResultList();
	}

	/**
	 * 按HQL查询唯一对象.
	 * 
	 * @param values
	 *            数量可变的参数,按顺序绑定.
	 */
	public <X> X findUnique(final String hql, final Object... values) {
		return (X) createQuery(hql, values).getSingleResult();
	}

	/**
	 * 按HQL查询唯一对象.
	 * 
	 * @param values
	 *            命名参数,按名称绑定.
	 */
	public <X> X findUnique(final String hql, final Map<String, ?> values) {
		return (X) createQuery(hql, values).getSingleResult();
	}

	/**
	 * 执行HQL进行批量修改/删除操作.
	 * 
	 * @param values
	 *            数量可变的参数,按顺序绑定.
	 * @return 更新记录数.
	 */
	@Transactional(readOnly = false)
	public int batchExecute(final String hql, final Object... values) {
		return createQuery(hql, values).executeUpdate();
	}

	/**
	 * 执行HQL进行批量修改/删除操作.
	 * 
	 * @param values
	 *            命名参数,按名称绑定.
	 * @return 更新记录数.
	 */
	@Transactional(readOnly = false)
	public int batchExecute(final String hql, final Map<String, ?> values) {
		return createQuery(hql, values).executeUpdate();
	}

	/**
	 * 根据查询HQL与参数列表创建Query对象. 与find()函数可进行更加灵活的操作.
	 * 
	 * @param values
	 *            数量可变的参数,按顺序绑定.
	 */
	public Query createQuery(final String queryString, final Object... values) {
		Assert.hasText(queryString, "queryString不能为空");
		Query query = em.createQuery(queryString);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				query.setParameter(i, values[i]);
			}
		}
		return query;
	}

	/**
	 * 根据查询HQL与参数列表创建Query对象. 与find()函数可进行更加灵活的操作.
	 * 
	 * @param values
	 *            命名参数,按名称绑定.
	 */
	public Query createQuery(final String queryString, final Map<String, ?> values) {
		Assert.hasText(queryString, "queryString不能为空");
		Query query = em.createQuery(queryString);
		if (values != null) {
			for (String k : values.keySet())
				query.setParameter(k, values.get(k));
		}
		return query;
	}

	/**
	 * Flush当前Session.
	 */
	public void flush() {
		em.flush();
	}

	/**
	 * Clear当前Session.
	 */
	public void clear() {
		em.clear();
	}

	/**
	 * 使entity脱离管理，不再和数据库同步.
	 */
	public void evict(Object entity) {
		em.detach(entity);
	}

}
