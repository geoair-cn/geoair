package cn.geoair.gtc.base.cache;

import cn.geoair.gtc.base.cache.support.GirMemoryCacheManager;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;

/**
 * 缓存操作工具类。如果要使用缓存注解，推荐使用JSR107定义的注解。
 * <p>
 * 该工具类提供了获取缓存实例的统一入口，并支持通过方法句柄机制进行扩展。 内置默认的内存缓存管理器实现。
 * </p>
 *
 * @author Ray
 * @since 1.0
 */
public class GirCacheHelper {

	/**
	 * 私有构造函数，防止实例化
	 */
	private GirCacheHelper() {
	}

	static {
		GkMethodHand.implFromClass(GirCacheHelper.class);
	}

	/**
	 * 获取指定名称的缓存实例
	 * <p>
	 * 通过方法句柄机制调用具体的缓存实现，支持扩展不同的缓存管理器。
	 * </p>
	 * @param name 缓存名称，用于标识特定的缓存实例
	 * @return GiCache 缓存实例接口
	 */
	@GaMethodHandDefine()
	public static GiCache getCache(String name) {
		return (GiCache) GkMethodHand.invokeSelf(name);
	}

	/**
	 * 内存缓存管理器实例
	 * <p>
	 * 提供基于内存的缓存实现，用于存储和管理缓存数据。
	 * </p>
	 */
	private static GirMemoryCacheManager memoryCacheManager = new GirMemoryCacheManager();

	/**
	 * 默认缓存获取实现
	 * <p>
	 * 提供一个基于内存的动态缓存管理器管理的缓存实现， 作为getCache方法的默认处理器。
	 * </p>
	 * @param name 缓存名称
	 * @return GiCache 内存缓存实例
	 */
	@GaMethodHandImpl(implClass = GirCacheHelper.class, implMethod = "getCache",
			type = GaMethodHandImpl.ImplType.comity)
	private static GiCache _getCache(String name) {
		return memoryCacheManager.getCache(name);
	}

}

/*
 * JSR107 定义注解说明
 *
 * @CacheResult 将指定的key和value映射内容存入到缓存容器中
 *
 * @CachePut 更新指定缓存容器中指定key值缓存记录内容
 *
 * @CacheRemove 移除指定缓存容器中指定key值对应的缓存记录
 *
 * @CacheRemoveAll 字面含义，移除指定缓存容器中的所有缓存记录
 *
 * @CacheKey 作为接口参数前面修饰，用于指定特定的入参作为缓存key值的组成部分
 *
 * @CacheValue 作为接口参数前面的修饰，用于指定特定的入参作为缓存value值
 *
 * JSR107 API核心组件说明
 *
 * CachingProvider：创建、配置、获取、管理和控制多个CacheManager
 * CacheManager：创建、配置、获取、管理和控制多个唯一命名的Cache。（一个CacheManager仅被一个CachingProvider所拥有）
 * Cache：一个类似Map的数据结构。（一个Cache仅被一个CacheManager所拥有） Entry：一个存储在Cache中的key-value对
 * Expiry：每一个存储在Cache中的条目有一个定义的有效期，过期后不可访问、更新、删除。缓存有效期可以通过ExpiryPolicy设置
 */
