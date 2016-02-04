package org.coral.core;

import org.coral.core.utils.ConfigurableConstants;

/**
 * 系统级静态常量. 可通过mip.properties初始化,同时保持常量 static & final的特征.
 * 
 * @see ConfigurableConstants
 * 
 */
public class Constants extends ConfigurableConstants {

	// 静态初始化读入springside.properties中的设置
	static {
		init("application.properties");
	}

	/**
	 * 默认用户密码
	 */
	public final static String DEFAULT_PASSWD = getProperty(
			"constant.default.password", "123456");
	
	public final static String[] PATTERNS = getProperty(
			"logger.patterns", "NONE").split(",");
	
	public static final Long STATUS_INVALID = new Long(0);

	public static final Long STATUS_VALID = new Long(1);
	
	/**
	 * 设置 Tomcat 服务器地址，如果为空，则取系统环境变量中"CATALINA_HOME"的定义
	 */
	public final static String CATALINA_HOME = getProperty("constant.catalina.home", "");	


}
