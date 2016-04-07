package org.coral.core.cache;

public interface EhCacheManager {
	<T> T get(String cacheName,String key);
	void put(String cacheName,String key,Object obj);
	void put(String cacheName,String key,Object obj,Integer timeToLiveSeconds);
	void remove(String cacheName,String key);
	boolean exist(String cacheName,String key);
}
