package cn.geoair.gtc.base.cache;

import java.util.concurrent.Callable;


/**
 * Cache 通用api，这里仿照spring cache而不是JSR是因为大部分缓存方案适配spring cache.
 * 简化了spring cache的ValueWrapper，加入了过期时间
 * @author Ray
 *
 */
public interface GiCache {

	/**
	 * 获取缓存名称
	 * @return 缓存名称
	 */
	String getName();

	/**
	 * 加入缓存，默认过期时间
	 * @param key 缓存键
	 * @param value 缓存值
	 */
	void put(Object key, Object value);

	/**
	 * 加入缓存，指定过期时间
	 * @param key 缓存键
	 * @param value 缓存值
	 * @param milliseconds 缓存过期时间，单位毫秒
	 */
	void put(Object key, Object value, long milliseconds);

	/**
	 * 获取缓存对象
	 * @param key 缓存键
	 * @return 缓存值对象
	 */
	Object getObject(Object key);

	/**
	 *  判断缓存是否存在
	 * @param key
	 * @return
	 */
	boolean exists(Object key);
	/**
	 * 获取指定类型的缓存值
	 * @param <T> 泛型类型
	 * @param key 缓存键
	 * @param type 期望返回的类型
	 * @return 指定类型的缓存值
	 */
	<T> T get(Object key, Class<T> type);

	/**
	 * 获取字符串类型的缓存值
	 * @param key 缓存键
	 * @return 字符串类型的缓存值
	 */
	String getString(Object key);

	/**
	 * 获取缓存值，如果不存在则通过Callable加载
	 * @param <T> 泛型类型
	 * @param key 缓存键
	 * @param valueLoader 值加载器
	 * @return 缓存值
	 * @throws Exception 加载异常
	 */
	<T> T get(Object key, Callable<T> valueLoader) throws Exception;

	/**
	 * 返回缓存剩余生存时间
	 * @param key 缓存键
	 * @return 剩余毫秒数，-1表示永不过期，-2表示键不存在
	 */
	long pttl(Object key);

	/**
	 * 清除指定key的缓存
	 * @param key 缓存键
	 */
	void evict(Object key);

	/**
	 * 清空所有缓存
	 */
	void clear();

}

/*
 * spring cache

public interface Cache {
	String getName();
	// 返回本地存储的那个。比如ConcurrentMapCache本地就是用的一个ConcurrentMap
	Object getNativeCache();

	// 就是用下面的ValueWrapper把值包装了一下而已~
	@Nullable
	ValueWrapper get(Object key);
	@Nullable
	<T> T get(Object key, @Nullable Class<T> type);
	@Nullable
	<T> T get(Object key, Callable<T> valueLoader);

	void put(Object key, @Nullable Object value);
	// @since 4.1
	// 不存在旧值直接put就先去了返回null，否则返回旧值（并且不会把新值put进去）
	@Nullable
	ValueWrapper putIfAbsent(Object key, @Nullable Object value);
	// 删除
	void evict(Object key);
	// 清空
	void clear();


	@FunctionalInterface
	interface ValueWrapper {
		@Nullable
		Object get();
	}
}

*/
