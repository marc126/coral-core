package org.coral.core.web.grid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coral.core.dao.CriteriaSetup;
import org.coral.core.dao.CriteriaSetup.MatchType;
import org.coral.core.utils.BeanUtils;
import org.coral.core.utils.UtilDateTime;
import org.coral.core.utils.UtilString;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author marc
 * 
 */
public class QueryUtils {
	protected static final Log logger = LogFactory.getLog(QueryUtils.class);
	public static void buildFilter(PageRequest pr, CriteriaSetup cs) {
		if (pr.isSearch()) {
			if (UtilString.isNotEmpty(pr.getFilters())) {// 多条件查询
				buildMultipleFilter(pr, cs);
			} else {// 单字段查询
				buildSimpleFilter(pr, cs);
			}
		}
	}
	
	private static void buildSimpleFilter(PageRequest pr, CriteriaSetup cs) {
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
			cs.addFilter(pr.getSearchField(), realSearch,MatchType.EQ);
			break;
		case "ne":// 不等
			cs.addFilter(pr.getSearchField(), realSearch,MatchType.NE);
			break;
		case "lt":// 小于
			cs.addFilter(pr.getSearchField(), realSearch,MatchType.LT);
			break;
		case "le":// 小于等于
			cs.addFilter(pr.getSearchField(), realSearch,MatchType.LE);
			break;
		case "gt":// 大于
			cs.addFilter(pr.getSearchField(), realSearch,MatchType.GT);
			break;
		case "ge":// 大于等于
			cs.addFilter(pr.getSearchField(), realSearch,MatchType.GE);
			break;
		case "bw":// 开始于
			cs.addFilter(pr.getSearchField(),pr.getSearchString(),MatchType.BEGIN);
			break;
		case "bn":// 不开始于
			cs.addFilter(pr.getSearchField(), pr.getSearchString(),
					MatchType.NBEGIN);
			break;
		case "ew":// 结束于
			cs.addFilter(pr.getSearchField(), pr.getSearchString(),
					MatchType.END);
			break;
		case "en":// 不结束于
			cs.addFilter(pr.getSearchField(), pr.getSearchString(),
					MatchType.NEND);
			break;
		case "cn":// 包含
			cs.addFilter(pr.getSearchField(), pr.getSearchString(),
					MatchType.LIKE);
			break;
		case "nc":// 不包含
			cs.addFilter(pr.getSearchField(), pr.getSearchString(),
					MatchType.NLIKE);
			break;
		case "nu":// 空
			cs.addFilter(pr.getSearchField(), realSearch, MatchType.NULL);
			break;
		case "nn":// 非空
			cs.addFilter(pr.getSearchField(), realSearch, MatchType.NNULL);
			break;
		case "in":// 属于
			cs.addFilter(pr.getSearchField(), pr.getSearchString().split(","),
					MatchType.IN);
			break;
		case "ni":// 不属于
			cs.addFilter(pr.getSearchField(), pr.getSearchString().split(","),
					MatchType.NIN);
			break;
		}
	}

	private static void buildMultipleFilter(PageRequest pr, CriteriaSetup cs) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode jn = mapper.readTree(pr.getFilters());
			MultipleFilter f = new QueryUtils().new MultipleFilter(jn);
			Criterion c = f.buildCriterion(pr,cs);
			if(c!=null)
				cs.addCriterion(c);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	public class MultipleFilter {
		public MultipleFilter(JsonNode node) {
			if (node.has("groupOp") && node.has("rules")) {
				this.groupOp = node.get("groupOp").asText();
				JsonNode rules = node.get("rules");
				Iterator<JsonNode> rulesIt = rules.elements();
				this.rules = new ArrayList<Filter>();
				while (rulesIt.hasNext()) {
					JsonNode r = rulesIt.next();
					this.rules.add(new Filter(r.get("field").asText(), r.get(
							"op").asText(), r.get("data").asText()));
				}
				if (node.has("groups")) {
					JsonNode groups = node.get("groups");
					Iterator<JsonNode> groupIt = groups.elements();
					this.groups = new ArrayList<MultipleFilter>();
					while (groupIt.hasNext()) {
						JsonNode r = groupIt.next();
						this.groups.add(new MultipleFilter(r));
					}
				}
			}
		}
		
		private String makeAlias(CriteriaSetup cs,String name){
			String fieldName = name;
			if(name.indexOf('.') != -1) {
				String lastNode = StringUtils.substringAfterLast(name, ".");
				String[] as = name.split("\\.");
				String alias = "";
				for(int i=0;i<as.length-1;i++){
					if("".equals(alias))
						alias +=as[i];
					else
						alias += "."+as[i];
					if(!cs.isExistAlias(alias))
						cs.addAlias(alias, as[i]);
					fieldName = as[i]+"."+lastNode;;
				}
			}
			return fieldName;
		}
		
		{
			DateConverter converter = new DateConverter();
			converter.setPatterns(new String[]{UtilDateTime.SIMPLEFORMATSTRING,UtilDateTime.DEFAULTFORMAT,UtilDateTime.SHORTFORMAT});
			ConvertUtils.register(converter, java.util.Date.class);
		}
		public Criterion buildCriterion(PageRequest pr,CriteriaSetup cs) {
			List<Criterion> total = new ArrayList<Criterion>();
			
			if (rules != null)
				for (Filter f : rules) {
					Object realSearch = null;
					try {
						if ("eq".equals(f.getOp()) || "ne".equals(f.getOp()) || "lt".equals(f.getOp()) || "le".equals(f.getOp()) || "gt".equals(f.getOp()) || "ge".equals(f.getOp())) {
							Class claz = BeanUtils.getMultiLayerType(pr.getEntity(), f.getField());
							
							if(BeanUtils.isBaseType(claz)){
								realSearch = ConvertUtils.convert(f.getData(), claz);
							}
						}
					} catch (Exception e) {
						logger.error("查询内容转换类别失败："+pr.getEntity()+":"+f.getField()+":"+f.getData());
						continue;
					}
					String field = makeAlias(cs,f.getField());
					Criterion one = null;
					switch (f.getOp()) {
					case "eq":// 等于
						one = Restrictions.eq(field, realSearch);
						break;
					case "ne":// 不等
						one = Restrictions.ne(field, realSearch);
						break;
					case "gt":// 大于
						one = Restrictions.gt(field, realSearch);
						break;
					case "ge":// 大于等于
						one = Restrictions.ge(field, realSearch);
						break;
					case "lt":// 小于
						one = Restrictions.lt(field, realSearch);
						break;
					case "le":// 小于等于
						one = Restrictions.le(field, realSearch);
						break;
					case "bw":// 开始于
						one = Restrictions.like(field, f.getData(), MatchMode.START);
						break;
					case "bn":// 不开始于
						one = Restrictions.not(Restrictions.like(field, f.getData(), MatchMode.START));
						break;
					case "ew":// 结束于
						one = Restrictions.like(field, f.getData(), MatchMode.END);
						break;
					case "en":// 不结束于
						one = Restrictions.not(Restrictions.like(field, f.getData(), MatchMode.END));
						break;
					case "cn":// 包含
						one = Restrictions.like(field, f.getData(), MatchMode.ANYWHERE);
						break;
					case "nc":// 不包含
						one = Restrictions.not(Restrictions.like(field, f.getData(), MatchMode.ANYWHERE));
						break;
					case "nu":// 空
						one = Restrictions.isNull(field);
						break;
					case "nn":// 非空
						one = Restrictions.isNotNull(field);
						break;
					case "in":// 属于
						one = Restrictions.in(field, f.getData().split(","));
						break;
					case "ni":// 不属于
						one = Restrictions.not(Restrictions.in(field, f.getData().split(",")));
						break;
					}
					if (one != null)
						total.add(one);

				}
			if (groups != null)
				for (MultipleFilter mf : groups) {
					total.add(mf.buildCriterion(pr, cs));
				}
			if (total.size() > 1) {
				if ("AND".equalsIgnoreCase(groupOp))
					return Restrictions.and(total.toArray(new Criterion[] {}));
				else
					return Restrictions.or(total.toArray(new Criterion[] {}));
			} else if (total.size() == 1) {
				return total.get(0);
			} else {
				return null;
			}
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
