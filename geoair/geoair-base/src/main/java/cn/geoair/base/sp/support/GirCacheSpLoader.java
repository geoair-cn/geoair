package cn.geoair.base.sp.support;

import java.lang.reflect.Type;
import java.util.Map;

import cn.geoair.base.sp.GkSpLoader;
import cn.geoair.base.tool.GkConcurrentReferenceHashMap;

/**
 * 带缓存的SpLoader父类
 *
 * @author Ray
 *
 */
public abstract class GirCacheSpLoader implements GkSpLoader {

	/**
	 * 默认的缓存key方式
	 * @param cls 类对象
	 * @param types 泛型类型数组
	 * @return 生成的缓存key字符串
	 */
	public static String strKeyForClassAndTypes(Class<?> cls, Type[] types) {
		String key = cls.getName();
		if (types != null && types.length > 0) {
			for (Type type : types) {
				key = key + "-" + type.getTypeName();
			}
		}
		return key;
	}

	// 实体缓存映射表，使用线程安全的并发引用哈希映射
	private final Map<String, Object> entityCacheMap = new GkConcurrentReferenceHashMap<String, Object>();

	/**
	 * 获取缓存
	 * @param <T> 泛型类型
	 * @param cls 类对象
	 * @param types 泛型类型数组
	 * @return 缓存的对象，如果不存在则返回null
	 */
	@Override
	public <T> T load(Class<T> cls, Type[] types) {
		String key = cacheKey(cls, types);
		Object obj = entityCacheMap.get(key);
		if (obj != null) {
			return (T) obj;
		}
		return null;
	}

	/**
	 * 设置缓存
	 * @param <T> 泛型类型
	 * @param cls 类对象
	 * @param types 泛型类型数组
	 * @param t 需要缓存的对象
	 */
	public <T> void setCache(Class<T> cls, Type[] types, T t) {
		String key = cacheKey(cls, types);
		entityCacheMap.put(key, t);
	}

	/**
	 * 缓存Key生成方法
	 * @param cls 类对象
	 * @param types 泛型类型数组
	 * @return 生成的缓存key字符串
	 */
	public String cacheKey(Class<?> cls, Type[] types) {

		return strKeyForClassAndTypes(cls, types);

	}

}
