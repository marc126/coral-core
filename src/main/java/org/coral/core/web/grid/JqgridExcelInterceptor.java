package org.coral.core.web.grid;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.coral.core.utils.UtilEncode;
import org.coral.core.utils.UtilString;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class JqgridExcelInterceptor implements HandlerInterceptor{
	protected static final Log logger = LogFactory.getLog(JqgridExcelInterceptor.class);
	//NamedThreadLocal<> d = new NamedThreadLocal<T>("");
	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod){
			HandlerMethod m = (HandlerMethod) handler;
			if(Page.class == m.getMethod().getReturnType()){
				String all = request.getParameter("all");
				String oper = request.getParameter("oper");
				if("excel".equals(oper)){
					if("true".equals(all))
						request.setAttribute("queryall",true);
					else
						request.setAttribute("queryall",false);
				
					if(m.getMethodParameters().length==1 && m.getMethodParameters()[0].getParameterType() == HttpServletRequest.class){
						Page rt = (Page) m.getMethod().invoke(m.getBean(), request);
						genExcel(request,response,rt);
						return false;
					}else{
						response.getWriter().println("不支持该表数据导出Excel！");
						logger.error("需要导出Excel请参照：Page<CoreUser> listAsJson(HttpServletRequest request),注：参数只能有且只有request一个，返回Page类型。");
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private void genExcel(HttpServletRequest request,
			HttpServletResponse response,Page page) throws Exception{
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = wb.createSheet("导出表格");
		String[] realCols = new String[]{};
		for(int i=0;i<page.getRows().size();i++){
			Map<String,Object> one = (Map<String, Object>) page.getRows().get(i);
			
			if(i==0){//生成第一行
				Row row = sheet.createRow(i);
				String cols = request.getParameter("colNames");
				if(UtilString.isNotEmpty(cols)){
					String[] col = UtilEncode.urlDecode(cols).split(",");
					for(int k=0;k<col.length;k++){
						row.createCell(k).setCellValue(col[k]);
					}
				}
				String rCols = request.getParameter("realCols");
				if(UtilString.isNotEmpty(rCols)){
					realCols = UtilEncode.urlDecode(rCols).split(",");
				}
				
			}
			Row row = sheet.createRow(i+1);
			int j = 0;
			for(String k:realCols){
				Object v = one.get(k);
				Cell c = row.createCell(j++);
				if(v==null)
					continue;
				c.setCellValue(ConvertUtils.convert(v));
			}
		}
		
		response.setHeader("Cache-Control", "no-cache");
		String showname = java.net.URLEncoder.encode("export"+new Date().getTime()+".xlsx","UTF-8");
		showname = showname.replace("+", "%20");
		response.setContentType("application/octet-stream; CHARSET=utf8");
		response.setHeader("Content-Disposition","attachment; filename="+showname);
		wb.write(response.getOutputStream());
	}
	
	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		
	}
	
}
