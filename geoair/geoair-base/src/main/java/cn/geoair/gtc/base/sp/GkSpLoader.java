package cn.geoair.gtc.base.sp;

import java.lang.reflect.Type;

/**
 * Service Provider loader 接口
 *
 * @author Ray
 *
 */
public interface GkSpLoader {

	public <T> T load(Class<T> cls, Type[] types);

	// private static final Map<Class<? extends GkSpLoader>, GkSpLoader> LOADER_CACHE =
	// new ConcurrentHashMap<Class<? extends GkSpLoader>, GkSpLoader>();

}
