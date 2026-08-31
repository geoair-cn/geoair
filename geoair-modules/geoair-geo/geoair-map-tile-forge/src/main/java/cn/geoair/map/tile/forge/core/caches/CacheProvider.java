package cn.geoair.map.tile.forge.core.caches;

import java.util.concurrent.Callable;

public interface CacheProvider {
    /**
     * 获取缓存名称
     *
     * @return
     */
    String getName();

    /**
     * 加入缓存
     *
     * @param key
     * @param value
     */
    void put(Object key, Object value);

    /**
     * 加入缓存
     *
     * @param key
     * @param value
     * @param milliseconds 缓存过期时间，单位 毫秒
     * @return
     */
    void put(Object key, Object value, long milliseconds);

    /**
     * 获取缓存
     *
     * @param key
     * @return
     */
    Object getObject(Object key);

    /**
     * 判断缓存是否存在
     *
     * @param key
     * @return
     */
    boolean exists(Object key);

    /**
     * 获取缓存 返回对应类型
     *
     * @param <T>
     * @param key
     * @param type
     * @return
     */
    <T> T get(Object key, Class<T> type);

    /**
     * 返回常用字符串缓存
     *
     * @param key
     * @return
     */
    String getString(Object key);

    /**
     * 获取缓存 返回对应类型
     *
     * @param <T>
     * @param key
     * @param valueLoader
     * @return
     */
    <T> T get(Object key, Callable<T> valueLoader);

    byte[] getByte(Object key);

    /**
     * 返回缓存剩余毫秒数
     *
     * @param key
     * @return 剩余毫秒数
     */
    long pttl(Object key);

    /**
     * 清除掉某个key的缓存
     *
     * @param key
     */
    void evict(Object key);
    /**
     * 清除掉某个前缀的缓存
     *
     * @param prefix
     */
    void evictByPreFix(Object prefix);

    /**
     * 清空缓存
     *
     * @return
     */
    void clear();
}
