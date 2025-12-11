package cn.geoair.gtc.spi.cache;

import cn.geoair.gtc.base.lang.invoke.GkMethodHand;

import java.util.concurrent.Callable;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import cn.geoair.gtc.base.cache.GiCache;
import cn.geoair.gtc.base.cache.GtcCacheHelper;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import  cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.gtc.base.util.GutilClass;

public class Cache4Gtc{

	public enum CacheType {SPRING,JSR,GW};


	private static CacheType cacheType;

	static {

		GkMethodHand.implFromClass(Cache4Gtc.class);
		if(GutilClass.isPresent("org.springframework.cache.CacheManager", SpringCache.class.getClassLoader())) {
			Cache4Gtc.setCacheType(CacheType.SPRING);
		}else
		if(GutilClass.isPresent("javax.cache.Cache", JSRCache.class.getClassLoader())) {
			Cache4Gtc.setCacheType(CacheType.JSR);
		}else {
			Cache4Gtc.setCacheType(CacheType.GW);
		}
	}


	public static void setCacheType(CacheType cacheType2) {
		cacheType = cacheType2;
	}


	@GaMethodHandImpl(implClass= GtcCacheHelper.class,implMethod="getCache",type=ImplType.expectfirst)
	public static GiCache getCache(String name) {
		switch (cacheType) {
		case SPRING:
			return SpringCache.createCache(name);
		case JSR:
			return JSRCache.createCache(name);
		default:
			//return  gtcConsoleCache.forName(name);
			return null;
		}
	}




	private static class SpringCache implements GiCache{

		private org.springframework.cache.CacheManager cacheManager;

		private SpringCache(String name) {

			org.springframework.cache.Cache cache = cacheManager.getCache(name);
		}

		public static GiCache createCache(String name) {
			return new SpringCache(name);
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void put(Object key, Object value) {
			// TODO Auto-generated method stub

		}

		@Override
		public void put(Object key, Object value, long milliseconds) {
			// TODO Auto-generated method stub

		}

		@Override
		public Object getObject(Object key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T get(Object key, Class<T> type) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getString(Object key) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T get(Object key, Callable<T> valueLoader) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long pttl(Object key) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void evict(Object key) {
			// TODO Auto-generated method stub

		}

		@Override
		public void clear() {
			// TODO Auto-generated method stub

		}




	}



	private static class JSRCache implements GiCache{



		private String cacheName;

		private Cache<Object, Object> cache;

		private JSRCache(String name) {
			cacheName = name;
			cache = Caching.getCache(name, Object.class, Object.class);
		}

		public static GiCache createCache(String name) {
			return new JSRCache(name);
		}

		@Override
		public String getName() {
			return cacheName;
		}

		@Override
		public void put(Object key, Object value) {
			cache.put(key, value);
		}

		@Override
		public void put(Object key, Object value, long milliseconds) {
			//cache.p
		}

		@Override
		public Object getObject(Object key) {
			return cache.get(key);
		}

		@Override
		public <T> T get(Object key, Class<T> type) {
			return cache.invoke(key, new EntryProcessor<Object, Object, T>() {
				@Override
				public T process(MutableEntry<Object, Object> entry ,Object... arguments) throws EntryProcessorException {
					return entry.unwrap((Class<T>)arguments[0]);
				}}, type);
		}

		@Override
		public String getString(Object key) {
			return cache.get(key).toString();
		}

		@Override
		public <T> T get(Object key, Callable<T> valueLoader) {
			return null;
		}

		@Override
		public long pttl(Object key) {
			return 0;
		}

		@Override
		public void evict(Object key) {
			cache.remove(key);
		}

		@Override
		public void clear() {
			cache.clear();
		}


	}

}
