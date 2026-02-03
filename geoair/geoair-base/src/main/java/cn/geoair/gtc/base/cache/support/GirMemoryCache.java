package cn.geoair.gtc.base.cache.support;

import java.util.concurrent.Callable;

import cn.geoair.gtc.base.cache.GiCache;

public class GirMemoryCache implements GiCache {

	protected static GirCacheMap<Object,Object> cacheMap = new GirCacheMap<Object,Object>();

	private String name;

	public GirMemoryCache() {
		name = this.getClass().getName();
	}

	public GirMemoryCache(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getString(Object key) {
		return (String)cacheMap.get(key);
	}

	@Override
	public Object getObject(Object key) {
		return cacheMap.get(key);
	}

	@Override
	public void put(Object key, Object value, long milliseconds) {
		cacheMap.put(key, value, milliseconds);

	}

	@Override
	public long pttl(Object key) {
		return cacheMap.pttl(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Class<T> type) {
		return (T)cacheMap.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		return (T)cacheMap.get(key);
	}

	@Override
	public void put(Object key, Object value) {
		cacheMap.put(key, value);

	}

	@Override
	public void evict(Object key) {
		cacheMap.remove(key);
	}

	@Override
	public void clear() {
		cacheMap.clear();
	}

}
