package cn.geoair.gtc.base.cache.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import cn.geoair.gtc.base.cache.GiCache;

/**
 * 一个内存缓冲管理器，补位sping cache不存在的情况
 * @author Ray
 *
 */
public class GirMemoryCacheManager /*implements CacheManager*/ {

	private final ConcurrentMap<String, GiCache> cacheMap = new ConcurrentHashMap<>(16);

	private boolean dynamic = true;

	private boolean allowNullValues = true;

	private boolean storeByValue = false;


	public GirMemoryCacheManager() {
	}


	public GirMemoryCacheManager(String... cacheNames) {
		setCacheNames(Arrays.asList(cacheNames));
	}



	public void setCacheNames(Collection<String> cacheNames) {
		if (cacheNames != null) {
			for (String name : cacheNames) {
				this.cacheMap.put(name, createConcurrentMapCache(name));
			}
			this.dynamic = false;
		}
		else {
			this.dynamic = true;
		}
	}


	public void setAllowNullValues(boolean allowNullValues) {
		if (allowNullValues != this.allowNullValues) {
			this.allowNullValues = allowNullValues;
			recreateCaches();
		}
	}


	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	public void setStoreByValue(boolean storeByValue) {
		if (storeByValue != this.storeByValue) {
			this.storeByValue = storeByValue;
			recreateCaches();
		}
	}

	public boolean isStoreByValue() {
		return this.storeByValue;
	}


	//@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}

	//@Override
	public GiCache getCache(String name) {
		GiCache cache = this.cacheMap.get(name);
		if (cache == null && this.dynamic) {
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = createConcurrentMapCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		return cache;
	}

	private void recreateCaches() {
		for (Map.Entry<String, GiCache> entry : this.cacheMap.entrySet()) {
			entry.setValue(createConcurrentMapCache(entry.getKey()));
		}
	}

	protected GiCache createConcurrentMapCache(String name) {
		return new GirMemoryCache(name);
	}

}
