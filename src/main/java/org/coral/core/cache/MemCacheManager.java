package org.coral.core.cache;

import java.io.IOException;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.coral.core.exception.Assert;
import org.coral.core.utils.UtilString;

public class MemCacheManager implements CacheManager{
	MemcachedClient mc;
	String servers;
	public MemCacheManager(){
		
	}

	@Override
	public void put(String cname,String key, Object obj) {
		Assert.hasText(key);
		Assert.notNull(obj);
		try {
			mc.set(cname+"_"+key, 0, obj);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	@Override
	public void put(String cname,String key, Object obj,Integer expSeconds) {
		Assert.hasText(key);
		Assert.notNull(obj);
		try {
			mc.set(cname+"_"+key, expSeconds, obj);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	@Override
	public void remove(String cname,String key) {
		Assert.hasText(key);
		try{
			mc.delete(cname+"_"+key);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	@Override
	public <T> T  get(String cname,String key) {
		Assert.hasText(key);
		Object obj = null;
		try {
			obj = mc.get(cname+"_"+key);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return (T) obj;
	}

	@Override
	public boolean exist(String cname,String key) {
		if(UtilString.isEmpty(key))
			return false;
		try {
			return  mc.get(cname+"_"+key)!=null;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 
	}

	public void setServers(String servers) {
		this.servers = servers;
	}

	public MemcachedClient getMC(){
		return mc;
	}

	public void init() {
		try {
			if(!UtilString.isEmpty(servers)){
				XMemcachedClientBuilder memcachedClientBuilder = new XMemcachedClientBuilder(AddrUtil.getAddresses(servers));
				mc = memcachedClientBuilder.build();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void destroy() {
		if(mc != null){
			try {
				mc.shutdown();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mc=null;
		}
	}
}
