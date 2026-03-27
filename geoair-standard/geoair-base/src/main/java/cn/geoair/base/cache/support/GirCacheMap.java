package cn.geoair.base.cache.support;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个用Map作为缓存的缓存工具，支持设置键值对的过期时间
 *
 * @author Ray
 */
public class GirCacheMap<K, V> implements Map<K, V> {

    private Map<K, V> hashMap;

    private Map<K, Long> timeMap;

    private Long long_minus_1 = Long.valueOf(-1);

    /**
     * 构造方法，指定初始容量和负载因子
     *
     * @param initialCapacity 初始容量
     * @param loadFactor 负载因子
     */
    public GirCacheMap(int initialCapacity, float loadFactor) {
        hashMap = new ConcurrentHashMap<K, V>(initialCapacity, loadFactor);
        timeMap = new ConcurrentHashMap<K, Long>(initialCapacity, loadFactor);
    }

    /**
     * 构造方法，指定初始容量
     *
     * @param initialCapacity 初始容量
     */
    public GirCacheMap(int initialCapacity) {
        hashMap = new ConcurrentHashMap<K, V>(initialCapacity);
        timeMap = new ConcurrentHashMap<K, Long>(initialCapacity);
    }

    /** 默认构造方法 */
    public GirCacheMap() {
        hashMap = new ConcurrentHashMap<K, V>();
        timeMap = new ConcurrentHashMap<K, Long>();
    }
    /*
     * public CacheMap(Map<? extends K, ? extends V> m) { hashMap = new
     * ConcurrentHashMap<K,V>(m); timeMap = new ConcurrentHashMap<K,Long>(); }
     */

    /**
     * 设置键的过期时间
     *
     * @param key 键
     * @param milliseconds 过期时间，单位毫秒,如果设置0,等于删除，设置小于0等于设置永久
     * @return 返回键的过期时间单位毫秒，键没有过期时间返回 -1，不存在键返回0
     */
    public long expire(K key, long milliseconds) {
        if (milliseconds < 0) {
            return expire_(key, long_minus_1);
        } else {
            return expire_(key, Long.valueOf(milliseconds));
        }
    }

    /**
     * 设置键的过期时间的内部实现
     *
     * @param key 键
     * @param milliseconds 过期时间，单位毫秒,如果设置0等于删除，设置负数=永久
     * @return 返回键的过期时间单位毫秒，键没有过期时间返回 -1，不存在键返回0
     */
    private long expire_(K key, Long milliseconds) {
        Iterator<Map.Entry<K, Long>> it = timeMap.entrySet().iterator();
        Map.Entry<K, Long> entry;
        while (it.hasNext()) {
            entry = it.next();
            if (entry.getKey().equals(key)) {
                long tm = entry.getValue().longValue();
                if ((tm == -1 || tm - System.currentTimeMillis() > 0)
                        && milliseconds != null
                        && milliseconds.longValue() != 0) {
                    if (milliseconds.longValue() < 0) {
                        entry.setValue(long_minus_1);
                        return -1;
                    } else {
                        long lv = System.currentTimeMillis() + milliseconds.longValue();
                        entry.setValue(Long.valueOf(lv));
                        return lv;
                    }
                } else {
                    hashMap.remove(key);
                    it.remove();
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * 以毫秒为单位获取key的剩余时间
     *
     * @param key 键
     * @return milliseconds 单位毫秒，没有设置时间返回 -1,不存在键返回0
     */
    public long pttl(K key) {
        return pttl_(key);
    }

    /**
     * 以毫秒为单位获取key的剩余时间的内部实现
     *
     * @param key 键
     * @return milliseconds 单位毫秒，没有设置时间返回 -1,不存在键返回0
     */
    private long pttl_(K key) {
        Iterator<Map.Entry<K, Long>> it = timeMap.entrySet().iterator();
        Map.Entry<K, Long> entry;
        while (it.hasNext()) {
            entry = it.next();
            if (entry.getKey().equals(key)) {
                Long tm = entry.getValue();
                if (tm.longValue() < 0) {
                    return -1;
                }
                long dis = tm.longValue() - System.currentTimeMillis();
                if (dis > 0) {
                    return dis;
                } else {
                    hashMap.remove(key);
                    it.remove();
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * 添加键值对并设置过期时间
     *
     * @param key 键
     * @param value 值
     * @param milliseconds 毫秒值 0为删除，负数则设置永久
     * @return 之前的值，如果没有则返回null
     */
    public V put(K key, V value, long milliseconds) {
        if (value == null) {
            return null;
        }
        if (milliseconds > 0) {
            timeMap.put(key, Long.valueOf(System.currentTimeMillis() + milliseconds));
        } else if (milliseconds == 0) {
            remove_(key);
            return null;
        } else {
            timeMap.put(key, long_minus_1);
        }
        return hashMap.put(key, value);
    }

    /**
     * 检查并清理过期的键值对
     *
     * @return 有效键值对的数量
     */
    private int checkMap() {
        Iterator<Map.Entry<K, Long>> it = timeMap.entrySet().iterator();
        Map.Entry<K, Long> entry;
        int len = 0;
        while (it.hasNext()) {
            entry = it.next();
            long tm = entry.getValue().longValue();
            if (tm > 0 && tm - System.currentTimeMillis() <= 0) {
                hashMap.remove(entry.getKey());
                it.remove();
            } else {
                len++;
            }
        }
        return len;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取缓存中键值对的数量
     *
     * @return 键值对数量
     */
    @Override
    public int size() {
        return checkMap();
    }

    /**
     * 判断缓存是否为空
     *
     * @return 如果为空返回true，否则返回false
     */
    @Override
    public boolean isEmpty() {
        return checkMap() == 0;
    }

    /**
     * 判断缓存中是否包含指定的键
     *
     * @param key 键
     * @return 如果包含返回true，否则返回false
     */
    @Override
    public boolean containsKey(Object key) {
        if (timeMap.containsKey(key)) {
            long left = pttl_((K) key);
            return left > 0 || left == -1;
        }
        return false;
    }

    /**
     * 判断缓存中是否包含指定的值
     *
     * @param value 值
     * @return 如果包含返回true，否则返回false
     */
    @Override
    public boolean containsValue(Object value) {
        checkMap();
        return hashMap.containsValue(value);
    }

    /**
     * 根据键获取对应的值
     *
     * @param key 键
     * @return 对应的值，如果不存在或已过期返回null
     */
    @Override
    public V get(Object key) {
        if (containsKey(key)) {
            return hashMap.get(key);
        }
        return null;
    }

    /**
     * 添加键值对到缓存中（永久有效）
     *
     * @param key 键
     * @param value 值
     * @return 之前的值，如果没有则返回null
     */
    @Override
    public V put(K key, V value) {
        if (value == null) {
            return null;
        }
        timeMap.put(key, long_minus_1);
        return hashMap.put(key, value);
    }

    /**
     * 从缓存中移除指定键的键值对
     *
     * @param key 键
     * @return 被移除的值，如果不存在返回null
     */
    @Override
    public V remove(Object key) {
        if (timeMap.containsKey(key)) {
            return remove_((K) key);
        }
        return null;
    }

    /**
     * 从缓存中移除指定键的键值对的内部实现
     *
     * @param key 键
     * @return 被移除的值，如果不存在返回null
     */
    private V remove_(K key) {
        Iterator<Map.Entry<K, Long>> it = timeMap.entrySet().iterator();
        Map.Entry<K, Long> entry;
        while (it.hasNext()) {
            entry = it.next();
            if (entry.getKey().equals(key)) {
                long tm = entry.getValue().longValue();
                it.remove();
                if (tm < 0 || tm - System.currentTimeMillis() > 0) {
                    return hashMap.remove(key);
                } else {
                    hashMap.remove(key);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 将指定映射中的所有键值对添加到此缓存中
     *
     * @param m 包含要添加的键值对的映射
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (K st : m.keySet()) {
            timeMap.put(st, long_minus_1);
        }
        hashMap.putAll(m);
    }

    /** 清空缓存中的所有键值对 */
    @Override
    public void clear() {
        timeMap.clear();
        hashMap.clear();
    }

    /**
     * 获取缓存中所有键的集合
     *
     * @return 所有键的集合
     */
    @Override
    public Set<K> keySet() {
        checkMap();
        return hashMap.keySet();
    }

    /**
     * 获取缓存中所有值的集合
     *
     * @return 所有值的集合
     */
    @Override
    public Collection<V> values() {
        checkMap();
        return hashMap.values();
    }

    /**
     * 获取缓存中所有键值对的集合
     *
     * @return 所有键值对的集合
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        checkMap();
        return hashMap.entrySet();
    }
}
