package org.coral.core.utils;

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestUtil {

	/**
	 * 获得IP地址
	 * @param request
	 * @return
	 */
	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if(UtilString.isNotEmpty(ip)){
			String[] ips = UtilString.split(ip,",");
			if(ips!=null){
				for(String tmpip : ips){
					if(UtilString.isEmpty(tmpip))
						continue;
					tmpip = tmpip.trim();
					if(isIPAddr(tmpip) && !tmpip.startsWith("10.") && !tmpip.startsWith("192.168.") && !"127.0.0.1".equals(tmpip)){
						return tmpip.trim();
					}
				}
			}
		}
		if (!isIPAddr(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (!isIPAddr(ip)) {
			ip = request.getHeader("x-real-ip");
		}
		if (!isIPAddr(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (!isIPAddr(ip)) {
			ip = request.getRemoteAddr();
		}
		if(ip.indexOf('.')==-1)
			ip = "127.0.0.1";
		return ip;
	}
	/**
	 * 判断是否为搜索引擎
	 * @param req
	 * @return
	 */
	public static boolean isRobot(HttpServletRequest req){
		String ua = req.getHeader("user-agent");
		if(UtilString.isEmpty(ua)) return false;
		return (ua != null
				&& (ua.indexOf("Baiduspider") != -1 || ua.indexOf("Googlebot") != -1
						|| ua.indexOf("sogou") != -1
						|| ua.indexOf("sina") != -1
						|| ua.indexOf("iaskspider") != -1
						|| ua.indexOf("ia_archiver") != -1
						|| ua.indexOf("Sosospider") != -1
						|| ua.indexOf("YoudaoBot") != -1
						|| ua.indexOf("yahoo") != -1 
						|| ua.indexOf("yodao") != -1
						|| ua.indexOf("MSNBot") != -1
						|| ua.indexOf("spider") != -1
						|| ua.indexOf("Twiceler") != -1
						|| ua.indexOf("Sosoimagespider") != -1
						|| ua.indexOf("naver.com/robots") != -1
						|| ua.indexOf("Nutch") != -1
						|| ua.indexOf("spider") != -1));	
	}
	/**
	 * 获得request的参数
	 * 
	 * @param aRequest
	 * @return
	 */
	public static String getRequestParameters(HttpServletRequest aRequest) {
		Map m = aRequest.getParameterMap();
		return createQueryStringFromMap(m, "&").toString();
	}

	public static StringBuffer createQueryStringFromMap(Map m, String ampersand) {
		StringBuffer aReturn = new StringBuffer("");
		Set aEntryS = m.entrySet();
		Iterator aEntryI = aEntryS.iterator();

		while (aEntryI.hasNext()) {
			Map.Entry aEntry = (Map.Entry) aEntryI.next();
			Object o = aEntry.getValue();

			if (o == null) {
				append(aEntry.getKey(), "", aReturn, ampersand);
			} else if (o instanceof String) {
				append(aEntry.getKey(), o, aReturn, ampersand);
			} else if (o instanceof String[]) {
				String[] aValues = (String[]) o;

				for (int i = 0; i < aValues.length; i++) {
					append(aEntry.getKey(), aValues[i], aReturn, ampersand);
				}
			} else {
				append(aEntry.getKey(), o, aReturn, ampersand);
			}
		}

		return aReturn;
	}

	private static StringBuffer append(Object key, Object value,
			StringBuffer queryString, String ampersand) {
		if (queryString.length() > 0) {
			queryString.append(ampersand);
		}

		// Use encodeURL from Struts' RequestUtils class - it's JDK 1.3 and 1.4
		// compliant
		queryString.append(encodeURL(key.toString()));
		queryString.append("=");
		queryString.append(encodeURL(value.toString()));

		return queryString;
	}
	/**
	 * 判断字符串是否是一个IP地址
	 * @param addr
	 * @return
	 */
	public static boolean isIPAddr(String addr){
		if(UtilString.isEmpty(addr))
			return false;
		String[] ips = UtilString.split(addr, ".");
		if(ips.length != 4)
			return false;
		try{
			int ipa = Integer.parseInt(ips[0]);
			int ipb = Integer.parseInt(ips[1]);
			int ipc = Integer.parseInt(ips[2]);
			int ipd = Integer.parseInt(ips[3]);
			return ipa >= 0 && ipa <= 255 && ipb >= 0 && ipb <= 255 && ipc >= 0
					&& ipc <= 255 && ipd >= 0 && ipd <= 255;
		}catch(Exception e){}
		return false;
	}
	/**
	 * 设置cookie值
	 * 
	 * @param response
	 * @param name
	 * @param value
	 * @param path
	 * @return HttpServletResponse
	 */
	public static void setCookie(HttpServletResponse response, String name,
			String value, String path,int maxAge) {

		Cookie cookie = new Cookie(name, value);
		cookie.setSecure(false);
		cookie.setPath(path);
		cookie.setMaxAge(maxAge); // 30 days 3600 * 24 * 30
		response.addCookie(cookie);
	}

	/**
	 * 获得cookie值
	 * 
	 * @param request
	 * @param name
	 * @return
	 */
	public static Cookie getCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		Cookie returnCookie = null;

		if (cookies == null) {
			return returnCookie;
		}

		for (int i = 0; i < cookies.length; i++) {
			Cookie thisCookie = cookies[i];

			if (thisCookie.getName().equals(name)) {
				// cookies with no value do me no good!
				if (!thisCookie.getValue().equals("")) {
					returnCookie = thisCookie;

					break;
				}
			}
		}

		return returnCookie;
	}

	/**
	 * 删除cookie值
	 * 
	 * @param response
	 * @param cookie
	 * @param path
	 */
	public static void deleteCookie(HttpServletResponse response,
			Cookie cookie, String path) {
		if (cookie != null) {
			// Delete the cookie by setting its maximum age to zero
			cookie.setMaxAge(0);
			cookie.setPath(path);
			response.addCookie(cookie);
		}
	}

	/**
	 * 对URL进行UTF－8编码
	 * 
	 * @param url
	 * @return
	 */
	public static String encodeURL(String url) {
		return encodeURL(url, "UTF-8");
	}

	/**
	 * 对URL编码
	 * 
	 * @param url
	 * @param enc
	 *            编码类型
	 * @return
	 */
	public static String encodeURL(String url, String enc) {
		try {

			if (enc == null || enc.length() == 0) {
				enc = "UTF-8";
			}
			return URLEncoder.encode(url, enc);
		} catch (Exception e) {
			System.err.print(e);
		}
		return null;
	}

	/**
	 * @param request
	 * @param key
	 * @return
	 */
	public static String getAttribute(HttpServletRequest request, String key) {
		String str = request.getParameter(key);
		if (UtilString.isEmpty(str)) {
			str = (String) request.getAttribute(key);
		}
		return str;
	}
}
