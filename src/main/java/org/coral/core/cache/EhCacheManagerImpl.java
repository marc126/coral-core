package org.coral.core.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coral.core.exception.Assert;
import org.coral.core.utils.UtilString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;

public class EhCacheManagerImpl implements EhCacheManager {
	private Log logger = LogFactory.getLog(getClass());

	@Autowired
	private EhCacheCacheManager ehCacheManager;

	public Cache getCache(String cacheName) {
		Cache cache = ehCacheManager.getCacheManager().getCache(cacheName);
		if (cache == null) {
			logger.warn("Could not find cache config [" + cacheName + "], using default.");
			ehCacheManager.getCacheManager().addCacheIfAbsent(cacheName);
			cache = ehCacheManager.getCacheManager().getCache(cacheName);
		}
		return cache;
	}

	@Override
	public void put(String cname, String key, Object obj) {
		Assert.hasText(key);
		Assert.notNull(obj);
		Element element = new Element(key, obj);
		if (logger.isDebugEnabled()) {
			logger.debug("Cache put: " + element.getObjectKey());
		}
		getCache(cname).put(element);
	}

	@Override
	public void remove(String cname, String key) {
		Assert.hasText(key);
		if (logger.isDebugEnabled()) {
			logger.debug("Cache remove: " + key);
		}
		getCache(cname).remove(key);
	}

	@Override
	public <T> T get(String cname, String key) {
		Assert.hasText(key);
		Element element = getCache(cname).get(key);
		if (element == null) {
			return null;
		} else {
			return (T) element.getObjectValue();
		}
	}

	@Override
	public boolean exist(String cname, String key) {
		if (UtilString.isEmpty(key))
			return false;
		return getCache(cname).isKeyInCache(key);
	}

	@Override
	public void put(String cacheName, String key, Object obj,
			Integer timeToLiveSeconds) {
		Assert.hasText(key);
		Assert.notNull(obj);
		Element element = new Element(key, obj, false, 0, timeToLiveSeconds);
		if (logger.isDebugEnabled()) {
			logger.debug("Cache put: " + element.getObjectKey());
		}
		getCache(cacheName).put(element);

	}
}
