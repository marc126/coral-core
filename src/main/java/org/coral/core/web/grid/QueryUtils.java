package org.coral.core.web.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.persistence.criteria.Predicate;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coral.core.dao.QuerySetup;
import org.coral.core.utils.BeanUtils;
import org.coral.core.utils.UtilDateTime;
import org.coral.core.utils.UtilString;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * @author marc
 * 
 */
public class QueryUtils {
	protected static final Log logger = LogFactory.getLog(QueryUtils.class);
	
	public static void buildFilter(PageRequest pr, QuerySetup qs) {
		if (pr.isSearch()) {
			if (UtilString.isNotEmpty(pr.getFilters())) {// 多条件查询
				buildMultipleFilter(pr, qs);
			} else {// 单字段查询
				buildSimpleFilter(pr, qs);
			}
		}
	}
	private static void buildSimpleFilter(PageRequest pr, QuerySetup qs) {
		Object realSearch = null;
		try {
			if ("eq".equals(pr.getSearchOper())
					|| "ne".equals(pr.getSearchOper())
					|| "lt".equals(pr.getSearchOper())
					|| "le".equals(pr.getSearchOper())
					|| "gt".equals(pr.getSearchOper())
					|| "ge".equals(pr.getSearchOper())) {
				Class claz = BeanUtils.getMultiLayerType(pr.getEntity(), pr.getSearchField());
				if(BeanUtils.isBaseType(claz)){
					realSearch = ConvertUtils.convert(pr.getSearchString(),claz);
				}
			}
		} catch (Exception e) {
			logger.error("查询内容转换类别失败："+pr.getEntity()+":"+pr.getSearchField()+":"+pr.getSearchString());
			return;
		}
		switch (pr.getSearchOper()) {
		case "eq":// 等于
			qs.eq(pr.getSearchField(), realSearch);
			break;
		case "ne":// 不等
			qs.notEq(pr.getSearchField(), realSearch);
			break;
		case "lt":// 小于
			qs.lt(pr.getSearchField(), realSearch);
			break;
		case "le":// 小于等于
			qs.le(pr.getSearchField(), realSearch);
			break;
		case "gt":// 大于
			qs.gt(pr.getSearchField(), realSearch);
			break;
		case "ge":// 大于等于
			qs.ge(pr.getSearchField(), realSearch);
			break;
		case "bw":// 开始于
			qs.like(pr.getSearchField(), pr.getSearchString() + "%");
			break;
		case "bn":// 不开始于
			qs.notLike(pr.getSearchField(), pr.getSearchString()+"%");
			break;
		case "ew":// 结束于
			qs.like(pr.getSearchField(), "%"+pr.getSearchString());
			break;
		case "en":// 不结束于
			qs.notLike(pr.getSearchField(), "%"+ pr.getSearchString());
			break;
		case "cn":// 包含
			qs.like(pr.getSearchField(), pr.getSearchString());
			break;
		case "nc":// 不包含
			qs.notLike(pr.getSearchField(), pr.getSearchString());
			break;
		case "nu":// 空
			qs.isNull(pr.getSearchField());
			break;
		case "nn":// 非空
			qs.isNotNull(pr.getSearchField());
			break;
		case "in":// 属于
			qs.in(pr.getSearchField(), Arrays.asList(pr.getSearchString().split(",")));
			break;
		case "ni":// 不属于
			qs.notIn(pr.getSearchField(), Arrays.asList(pr.getSearchString().split(",")));
			break;
		}
	}
	
	private static void buildMultipleFilter(PageRequest pr, QuerySetup qs) {

		try {
			MultipleFilter f = new QueryUtils().new MultipleFilter(JSONObject.parseObject(pr.getFilters()));
			f.build(pr,qs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public class MultipleFilter {
		public MultipleFilter(JSONObject node) {
			if (node.containsKey("groupOp") && node.containsKey("rules")) {
				this.groupOp = node.getString("groupOp");
				JSONArray rules = node.getJSONArray("rules");
				this.rules = new ArrayList<Filter>();
				for(int i=0;i<rules.size();i++){
					JSONObject r = rules.getJSONObject(i);
					this.rules.add(new Filter(r.getString("field"), r.getString("op"), r.getString("data")));
				}
				this.groups = new ArrayList<MultipleFilter>();
				if(node.containsKey("groups")){
					JSONArray groups = node.getJSONArray("groups");
					for(int i=0;i<groups.size();i++){
						JSONObject r = groups.getJSONObject(i);
						this.groups.add(new MultipleFilter(r));
					}
				}
			}
		}
		
		{
			DateConverter converter = new DateConverter();
			converter.setPatterns(new String[]{UtilDateTime.SIMPLEFORMATSTRING,UtilDateTime.DEFAULTFORMAT,UtilDateTime.SHORTFORMAT});
			ConvertUtils.register(converter, java.util.Date.class);
		}
		public void build(PageRequest pr,QuerySetup qs) {
			List<Predicate> backup = qs.getPredicates();
			qs.setPredicates(new ArrayList());
			if (rules != null)
				for (Filter f : rules) {
					Object realSearch = null;
					boolean isDate = false;
					java.util.Date dateBegin = null,dateEnd = null;
					try {
						if ("eq".equals(f.getOp()) || "ne".equals(f.getOp()) || "lt".equals(f.getOp()) || "le".equals(f.getOp()) || "gt".equals(f.getOp()) || "ge".equals(f.getOp())) {
							Class<?> claz = BeanUtils.getMultiLayerType(pr.getEntity(), f.getField());
							if(java.util.Date.class.equals(claz) && f.getData().length()==10){
								isDate = true;
								dateBegin = UtilDateTime.toDataTime(f.getData()+" 00:00:00");
								dateEnd = UtilDateTime.toDataTime(f.getData()+" 23:59:59");
							}else if(BeanUtils.isBaseType(claz)){
								realSearch = ConvertUtils.convert(f.getData(), claz);
							}
							
						}
					} catch (Exception e) {
						logger.error("查询内容转换类别失败："+pr.getEntity()+":"+f.getField()+":"+f.getData());
						continue;
					}
					
					String field = f.getField();
					switch (f.getOp()) {
					case "eq":// 等于
						if(isDate)
							qs.between(field, dateBegin, dateEnd);
						else
							qs.eq(field, realSearch);
						break;
					case "ne":// 不等
						if(isDate)
							qs.notBetween(field, dateBegin, dateEnd);
						else
							qs.notEq(field, realSearch);
						break;
					case "gt":// 大于
						if(isDate)
							qs.gt(field,dateEnd);
						else
							qs.gt(field, realSearch);
						break;
					case "ge":// 大于等于
						if(isDate)
							qs.ge(field,dateBegin);
						else
							qs.ge(field, realSearch);
						break;
					case "lt":// 小于
						if(isDate)
							qs.lt(field,dateBegin);
						else
							qs.lt(field, realSearch);
						break;
					case "le":// 小于等于
						if(isDate)
							qs.le(field,dateEnd);
						else
							qs.le(field, realSearch);
						break;
					case "bw":// 开始于
						qs.like(field, f.getData()+"%");
						break;
					case "bn":// 不开始于
						qs.notLike(field, f.getData()+"%");
						break;
					case "ew":// 结束于
						qs.like(field, "%"+f.getData());
						break;
					case "en":// 不结束于
						qs.notLike(field, "%"+f.getData());
						break;
					case "cn":// 包含
						qs.like(field, f.getData());
						break;
					case "nc":// 不包含
						qs.notLike(field, f.getData());
						break;
					case "nu":// 空
						qs.isNull(field);
						break;
					case "nn":// 非空
						qs.isNotNull(field);
						break;
					case "in":// 属于
						qs.in(field, Arrays.asList(f.getData().split(",")));
						break;
					case "ni":// 不属于
						qs.notIn(field, Arrays.asList(f.getData().split(",")));
						break;
					}

				}
			if (groups != null)
				for (MultipleFilter mf : groups) {
					mf.build(pr, qs);
				}
			if (qs.getPredicates().size() > 1) {
				if ("OR".equalsIgnoreCase(groupOp)){
					backup.add(qs.getCriteriaBuilder().or(qs.getPredicates().toArray(new Predicate[0])));
					qs.setPredicates(new ArrayList());
				}
			}
			qs.addCriterions(backup.toArray(new Predicate[0]));
			
		}

		String groupOp;
		List<Filter> rules;
		List<MultipleFilter> groups;

		public String getGroupOp() {
			return groupOp;
		}

		public void setGroupOp(String groupOp) {
			this.groupOp = groupOp;
		}

		public List<Filter> getRules() {
			return rules;
		}

		public void setRules(List<Filter> rules) {
			this.rules = rules;
		}

		public List<MultipleFilter> getGroups() {
			return groups;
		}

		public void setGroups(List<MultipleFilter> groups) {
			this.groups = groups;
		}

		public class Filter {
			public Filter(String field, String op, String data) {
				this.field = field;
				this.op = op;
				this.data = data;
			}

			String field;
			String op;
			String data;

			public String getField() {
				return field;
			}

			public void setField(String field) {
				this.field = field;
			}

			public String getOp() {
				return op;
			}

			public void setOp(String op) {
				this.op = op;
			}

			public String getData() {
				return data;
			}

			public void setData(String data) {
				this.data = data;
			}

		}
	}

}
