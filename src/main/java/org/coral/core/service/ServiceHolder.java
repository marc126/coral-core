package org.coral.core.service;

import org.coral.core.exception.Assert;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 配合自定义ContextLoaderListener使用的工具类
 * @author marc
 * @see org.coral.core.web.listener.ContextLoaderListener
 */
public class ServiceHolder implements ApplicationContextAware,DisposableBean {
	
	private static ApplicationContext applicationContext = null;

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		ServiceHolder.applicationContext = applicationContext;
	}

	protected ServiceHolder() { // 不允许实例化，全部使用static函数。
		
	}

	public static <T> T getBean(String beanName){
		assertContextInjected();
		return (T) applicationContext.getBean(beanName);
	}
	
	public static <T> T getBeanByType(Class<T> beanClass){
		assertContextInjected();
		return (T) applicationContext.getBeansOfType(beanClass);
	}

	@Override
	public void destroy() throws Exception {
		applicationContext = null;
	}
	
	private static void assertContextInjected() {
		Assert.isTrue(applicationContext != null,"applicaitonContext未注入,请在applicationContext.xml中定义ServiceHolder");
	}
}

