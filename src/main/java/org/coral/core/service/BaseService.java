package org.coral.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coral.core.dao.CriteriaSetup;
import org.coral.core.dao.CriteriaSetup.OrderType;
import org.coral.core.entity.BaseEntity;
import org.coral.core.exception.Assert;
import org.coral.core.exception.BizException;
import org.coral.core.utils.BeanUtils;
import org.coral.core.utils.GenericsUtils;
import org.coral.core.utils.UtilString;
import org.coral.core.web.grid.Page;
import org.coral.core.web.grid.PageRequest;
import org.coral.core.web.grid.QueryUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public abstract class BaseService<T extends BaseEntity> {
	protected static final Log logger = LogFactory.getLog(BaseService.class);
	@PersistenceContext
	private EntityManager em;
	private Class<T> entityClass;

	protected Class<T> getEntityClass() {
		if (entityClass == null)
			entityClass = GenericsUtils.getSuperClassGenricType(getClass(),0);
		return entityClass;
	}

	protected Session getSession() {
		return (Session) em.getDelegate();
	}

	public Page<T> findPage(CriteriaSetup base, PageRequest pr) {
		int start = (pr.getPage() - 1) * pr.getRows();
		Criteria criteria = getSession().createCriteria(getEntityClass());
		//转换过滤条件
		QueryUtils.buildFilter(pr, base);
		if (UtilString.isNotEmpty(pr.getSidx())) {
			base.clearSort();
			if ("asc".equals(pr.getSord()))
				base.addSort(pr.getSidx(), OrderType.ASC);
			else
				base.addSort(pr.getSidx(), OrderType.DESC);
		}
		base.setup(criteria);

		CriteriaImpl impl = (CriteriaImpl) criteria;
		// 先把Projection和OrderBy条件取出来,清空两者来执行Count操作
		Projection projection = impl.getProjection();
		List<CriteriaImpl.OrderEntry> orderEntries;
		try {
			orderEntries = (List) BeanUtils.forceGetProperty(impl,"orderEntries");
			BeanUtils.forceSetProperty(impl, "orderEntries", new ArrayList());
		} catch (Exception e) {
			throw new BizException("分页处理order时候异常");
		}
		long totalCount = 0;
		try {
			totalCount = (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
		}catch (Exception e) {
			logger.error("处理分页查询时出错，请检查输入参数！"+criteria.toString());
			return null;
		}
		// 将之前的Projection和OrderBy条件重新设回去
		criteria.setProjection(projection);
		if (projection == null) {
			criteria.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
		}
		//criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		try {
			BeanUtils.forceSetProperty(impl, "orderEntries", orderEntries);
		} catch (Exception e) {
			throw new InternalError(" Runtime Exception impossibility throw ");
		}

		List<T> list = criteria.setFirstResult(start).setMaxResults(pr.getRows()).list();
		Page<T> page = new Page<T>(list, pr.getFields(), pr.getPage(),totalCount, pr.getRows());
		return page;
	}
	/**
	 * 得到物理表名
	 * @param <T>
	 * @param entityClass
	 * @return
	 */
	public <T> String getTableName(Class<T> entityClass){
		SingleTableEntityPersister ep =  (SingleTableEntityPersister) getSession().getSessionFactory().getClassMetadata(entityClass);
		return ep.getTableName();
	}
	/**
	 * 得到物理字段名
	 * @param <T>
	 * @param entityClass
	 * @param propertyName
	 * @return
	 */
	public <T> String getColumnName(Class<T> entityClass,String propertyName){
		SingleTableEntityPersister ep =  (SingleTableEntityPersister) getSession().getSessionFactory().getClassMetadata(entityClass);
		return ep.getPropertyColumnNames(propertyName)[0];
	}
	/**
	 * 判断实体是否存在
	 * @param entity
	 * @return
	 */
	public Boolean containsEntity(Object entity){
		return em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(entity);
	}
	
	/**
	 * 取得对象的主键名.
	 */
	public String getIdName() {
		ClassMetadata meta = getSession().getSessionFactory().getClassMetadata(getEntityClass());
		return meta.getIdentifierPropertyName();
	}
	/**
	 * 保存新增或修改的对象.
	 */
	@Transactional(readOnly=false)
	public void save(final T entity) {
		Assert.notNull(entity, "entity不能为空");
		getSession().saveOrUpdate(entity);
		logger.debug("save entity: "+ entity.toString());
	}

	/**
	 * 删除对象.
	 * 
	 * @param entity 对象必须是session中的对象或含id属性的transient对象.
	 */
	@Transactional(readOnly=false)
	public void delete(final T entity) {
		Assert.notNull(entity, "entity不能为空");
		getSession().delete(entity);
		logger.debug("save entity: "+ entity.toString());
	}

	/**
	 * 按id删除对象.
	 */
	@Transactional(readOnly=false)
	public void delete(final Serializable id) {
		Assert.notNull(id, "id不能为空");
		delete(get(id));
		logger.debug("delete entity "+getEntityClass().getSimpleName()+" ,id is "+id);
	}
	
	/**
	 * 批量删除
	 */
	@Transactional(readOnly=false)
	public void batchDelete(final Serializable[] ids){
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Serializable id : ids) {
			sb.append(id + ",");
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append(")");
		batchExecute("delete from "+getEntityClass().getSimpleName()+" where "+getIdName()+" in "+sb);
	}

	/**
	 * 按id获取对象.
	 */
	public T get(final Serializable id) {
		Assert.notNull(id, "id不能为空");
		return (T) getSession().get(getEntityClass(), id);
	}

	/**
	 * 按id列表获取对象列表.
	 */
	public List<T> get(final Collection<Serializable> ids) {
		return find(Restrictions.in(getIdName(), ids));
	}

	/**
	 *	获取全部对象.
	 */
	public List<T> getAll() {
		return find();
	}

	/**
	 *	获取全部对象, 支持按属性行序.
	 */
	public List<T> getAll(String orderByProperty, boolean isAsc) {
		Criteria c = createCriteria();
		if (isAsc) {
			c.addOrder(Order.asc(orderByProperty));
		} else {
			c.addOrder(Order.desc(orderByProperty));
		}
		return c.list();
	}

	/**
	 * 按属性查找对象列表, 匹配方式为相等.
	 */
	public List<T> findBy(final String propertyName, final Object value) {
		Assert.hasText(propertyName, "propertyName不能为空");
		Criterion criterion = Restrictions.eq(propertyName, value);
		return find(criterion);
	}
	
	/**
	 * 按属性查询,并按某个属性排序
	 */
	public List<T> findBy(final String propertyName, final Object value,String orderByProperty,boolean isAsc) {
		Assert.hasText(propertyName, "propertyName不能为空");
		Criteria c = createCriteria();
		if (isAsc) {
			c.addOrder(Order.asc(orderByProperty));
		} else {
			c.addOrder(Order.desc(orderByProperty));
		}
		Criterion criterion = Restrictions.eq(propertyName, value);
		c.add(criterion);
		return c.list();
	}

	/**
	 * 按属性查找唯一对象, 匹配方式为相等.
	 */
	public T findUniqueBy(final String propertyName, final Object value) {
		Assert.hasText(propertyName, "propertyName不能为空");
		Criterion criterion = Restrictions.eq(propertyName, value);
		return (T) createCriteria(criterion).uniqueResult();
	}

	/**
	 * 按HQL查询对象列表.
	 * 
	 * @param values 数量可变的参数,按顺序绑定.
	 */
	public <X> List<X> find(final String hql, final Object... values) {
		return createQuery(hql, values).list();
	}

	/**
	 * 按HQL查询对象列表.
	 * 
	 * @param values 命名参数,按名称绑定.
	 */
	public <X> List<X> find(final String hql, final Map<String, ?> values) {
		return createQuery(hql, values).list();
	}

	/**
	 * 按HQL查询唯一对象.
	 * 
	 * @param values 数量可变的参数,按顺序绑定.
	 */
	public <X> X findUnique(final String hql, final Object... values) {
		return (X) createQuery(hql, values).uniqueResult();
	}

	/**
	 * 按HQL查询唯一对象.
	 * 
	 * @param values 命名参数,按名称绑定.
	 */
	public <X> X findUnique(final String hql, final Map<String, ?> values) {
		return (X) createQuery(hql, values).uniqueResult();
	}

	/**
	 * 执行HQL进行批量修改/删除操作.
	 * 
	 * @param values 数量可变的参数,按顺序绑定.
	 * @return 更新记录数.
	 */
	@Transactional(readOnly=false)
	public int batchExecute(final String hql, final Object... values) {
		return createQuery(hql, values).executeUpdate();
	}

	/**
	 * 执行HQL进行批量修改/删除操作.
	 * 
	 * @param values 命名参数,按名称绑定.
	 * @return 更新记录数.
	 */
	@Transactional(readOnly=false)
	public int batchExecute(final String hql, final Map<String, ?> values) {
		return createQuery(hql, values).executeUpdate();
	}

	/**
	 * 根据查询HQL与参数列表创建Query对象.
	 * 与find()函数可进行更加灵活的操作.
	 * 
	 * @param values 数量可变的参数,按顺序绑定.
	 */
	public Query createQuery(final String queryString, final Object... values) {
		Assert.hasText(queryString, "queryString不能为空");
		Query query = getSession().createQuery(queryString);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				query.setParameter(i, values[i]);
			}
		}
		return query;
	}

	/**
	 * 根据查询HQL与参数列表创建Query对象.
	 * 与find()函数可进行更加灵活的操作.
	 * 
	 * @param values 命名参数,按名称绑定.
	 */
	public Query createQuery(final String queryString, final Map<String, ?> values) {
		Assert.hasText(queryString, "queryString不能为空");
		Query query = getSession().createQuery(queryString);
		if (values != null) {
			query.setProperties(values);
		}
		return query;
	}

	/**
	 * 按Criteria查询对象列表.
	 * 
	 * @param criterions 数量可变的Criterion.
	 */
	public List<T> find(final Criterion... criterions) {
		return createCriteria(criterions).list();
	}

	/**
	 * 按Criteria查询唯一对象.
	 * 
	 * @param criterions 数量可变的Criterion.
	 */
	public T findUnique(final Criterion... criterions) {
		return (T) createCriteria(criterions).uniqueResult();
	}

	/**
	 * 根据Criterion条件创建Criteria.
	 * 与find()函数可进行更加灵活的操作.
	 * 
	 * @param criterions 数量可变的Criterion.
	 */
	public Criteria createCriteria(final Criterion... criterions) {
		Criteria criteria = getSession().createCriteria(getEntityClass());
		for (Criterion c : criterions) {
			criteria.add(c);
		}
		return criteria;
	}

	/**
	 * Flush当前Session.
	 */
	public void flush() {
		getSession().flush();
	}

	/**
	 * 为Query添加distinct transformer.
	 * 预加载关联对象的HQL会引起主对象重复, 需要进行distinct处理.
	 */
	public Query distinct(Query query) {
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query;
	}

	/**
	 * 为Criteria添加distinct transformer.
	 * 预加载关联对象的HQL会引起主对象重复, 需要进行distinct处理.
	 */
	public Criteria distinct(Criteria criteria) {
		criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return criteria;
	}
}
