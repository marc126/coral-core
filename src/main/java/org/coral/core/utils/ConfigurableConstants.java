package org.coral.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 可用Properties文件配置的Constants基类.
 * 本类既保持了Constants的static和final(静态与不可修改)特性,又拥有了可用Properties文件配置的特征,
 * 主要是应用了Java语言中静态初始化代码的特性.
 * <p/>
 * 子类可如下编写
 * <pre>
 * public class Constants extends ConfigurableConstants {
 *  static {
 *    init("test.properties");
 * }
 * <p/>
 * public final static String ERROR_BUNDLE_KEY = getProperty("constant.error_bundle_key", "errors"); }
 * </pre>
 *
 */
public class ConfigurableConstants {
    protected static Log logger = LogFactory.getLog(ConfigurableConstants.class);
    protected static Properties p = new Properties();
    
    /**
     * 静态读入属性文件到Properties p变量中
     */
    protected static void init(String propertyFileName) {
        InputStream in = null;
        try {
            in = ConfigurableConstants.class.getClassLoader().getResourceAsStream(propertyFileName);
            if (in != null){
                p.load(in);
            }
        } catch (IOException e) {
            logger.error("load " + propertyFileName + " into Constants error!");
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("close " + propertyFileName + " error!");
                }
            }
        }
    }

    /**
     * 封装了Properties类的getProperty函数,使p变量对子类透明.
     *
     * @param key          property key.
     * @param defaultValue 当使用property key在properties中取不到值时的默认值.
     */
    protected static String getProperty(String key, String defaultValue) {
        return p.getProperty(key, defaultValue);
    }
    
    protected static String getProperty(String key) {
        return p.getProperty(key);
    }

    protected static String findValue(String... keys) {
        for (String key : keys) {
            String value = getProperty(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    protected static boolean getBoolean(String key, boolean defaultVal) {
        String val = getProperty(key);
        return val == null ? defaultVal : Boolean.parseBoolean(val);
    }

    protected static long getLong(String key, long defaultVal) {
        String val = getProperty(key);
        return val == null ? defaultVal : Long.parseLong(val);
    }

    protected static int getInt(String key, int defaultVal) {
        return (int) getLong(key, defaultVal);
    }

    protected static double getDouble(String key, double defaultVal) {
        String val = getProperty(key);
        return val == null ? defaultVal : Double.parseDouble(val);
    }

    protected static <T extends Enum<T>> T getEnum(String key, Class<T> type, T defaultValue) {
        String val = getProperty(key);
        return val == null ? defaultValue : Enum.valueOf(type, val);
    }
}
