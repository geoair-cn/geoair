package cn.geoair.spi.cache;

import cn.geoair.base.cache.GiCache;
import cn.geoair.base.cache.GirCacheHelper;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.base.util.GutilClass;
import java.util.concurrent.Callable;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

public class Cache4Gir {

    public enum CacheType {
        SPRING,
        JSR,
        GW
    };

    private static CacheType cacheType;

    static {
        GkMethodHand.implFromClass(Cache4Gir.class);
        GirCacheHelper.setCacheProvider(Cache4Gir::getCache);
        if (GutilClass.isPresent(
                "org.springframework.cache.CacheManager", SpringCache.class.getClassLoader())) {
            Cache4Gir.setCacheType(CacheType.SPRING);
        } else if (GutilClass.isPresent("javax.cache.Cache", JSRCache.class.getClassLoader())) {
            Cache4Gir.setCacheType(CacheType.JSR);
        } else {
            Cache4Gir.setCacheType(CacheType.GW);
        }
    }

    public static void setCacheType(CacheType cacheType2) {
        cacheType = cacheType2;
    }

    @GaMethodHandImpl(
        implClass = GirCacheHelper.class,
        implMethod = "getCache",
        type = ImplType.expectfirst
    )
    public static GiCache getCache(String name) {
        switch (cacheType) {
            case SPRING:
                return SpringCache.createCache(name);
            case JSR:
                return JSRCache.createCache(name);
            default:
                // return gtcConsoleCache.forName(name);
                return null;
        }
    }

    private static class SpringCache implements GiCache {

        private org.springframework.cache.Cache springCache;

        private SpringCache(String name) {
            org.springframework.cache.CacheManager cacheManager = SpringCacheManagerProvider.getCacheManager();
            this.springCache = cacheManager.getCache(name);
        }

        public static GiCache createCache(String name) {
            return new SpringCache(name);
        }

        @Override
        public String getName() {
            return springCache.getName();
        }

        @Override
        public void put(Object key, Object value) {
            springCache.put(key, value);
        }

        @Override
        public void put(Object key, Object value, long milliseconds) {
            // Spring Cache不直接支持过期时间，需要额外处理
            // 可以考虑使用RedisTemplate或其他支持TTL的缓存实现
            springCache.put(key, value);
        }

        @Override
        public Object getObject(Object key) {
            org.springframework.cache.Cache.ValueWrapper wrapper = springCache.get(key);
            return wrapper != null ? wrapper.get() : null;
        }

        @Override
        public boolean exists(Object key) {
            return springCache.get(key) != null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            org.springframework.cache.Cache.ValueWrapper wrapper = springCache.get(key);
            return wrapper != null ? type.cast(wrapper.get()) : null;
        }

        @Override
        public String getString(Object key) {
            Object value = getObject(key);
            return value != null ? value.toString() : null;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            try {
                return springCache.get(key, valueLoader);
            } catch (Exception e) {
                throw new RuntimeException("加载缓存值失败", e);
            }
        }

        @Override
        public long pttl(Object key) {
            // Spring Cache不直接支持PTTL，返回-1表示永不过期
            return -1;
        }

        @Override
        public void evict(Object key) {
            springCache.evict(key);
        }

        @Override
        public void clear() {
            springCache.clear();
        }
    }

    private static class JSRCache implements GiCache {

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
            // cache.p
        }

        @Override
        public Object getObject(Object key) {
            return cache.get(key);
        }

        @Override
        public boolean exists(Object key) {
            return cache.containsKey(key);
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            return cache.invoke(
                    key,
                    new EntryProcessor<Object, Object, T>() {
                        @Override
                        public T process(MutableEntry<Object, Object> entry, Object... arguments)
                                throws EntryProcessorException {
                            return entry.unwrap((Class<T>) arguments[0]);
                        }
                    },
                    type);
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
