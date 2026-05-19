package cn.geoair.base.sp;

import cn.geoair.base.exception.GirException;
import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.sp.support.GirPlaceHolderSpLoader;
import cn.geoair.base.tool.GkConcurrentReferenceHashMap;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service Provider 工具类
 *
 * <p>提供服务发现与加载功能，支持基于注解的SPI机制，可以根据接口类型动态加载其实现类。 支持单例模式缓存，避免重复创建实例，提高系统性能。
 *
 * @author Ray
 */
public class GirSpHelper {

    /** 私有构造函数，防止外部实例化 */
    private GirSpHelper() {}

    /**
     * GkSpLoader加载器缓存
     *
     * <p>用于缓存已创建的GkSpLoader实例，避免重复创建，提高性能。 Key为Loader类类型，Value为对应的Loader实例。
     */
    private static final Map<Class<? extends GkSpLoader>, GkSpLoader> LOADER_CACHE =
            new ConcurrentHashMap<Class<? extends GkSpLoader>, GkSpLoader>();

    /** 重入锁，用于保证Loader实例创建的线程安全 */
    private static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * 获取GkSpLoader实例
     *
     * <p>首先从缓存中查找指定类型的Loader实例，如果不存在则创建新实例并加入缓存。 使用重入锁保证多线程环境下的线程安全。
     *
     * @param loaderClass Loader类类型
     * @return GkSpLoader实例
     * @throws GirException 当创建Loader实例失败时抛出异常
     */
    private static GkSpLoader getGkSpiLoader(Class<? extends GkSpLoader> loaderClass) {

        GkSpLoader loader;
        if (LOADER_CACHE.containsKey(loaderClass)) {
            loader = LOADER_CACHE.get(loaderClass);
        } else {
            LOCK.lock();
            try {
                LOADER_CACHE.put(loaderClass, loaderClass.newInstance());
                loader = LOADER_CACHE.get(loaderClass);
            } catch (Exception e) {
                throw new GirException(e, "创建GkSpLoader发生错误：{} ", loaderClass.getName());
            } finally {
                LOCK.unlock();
            }
        }
        return loader;
    }

    /**
     * 接口实现对象的缓存，采用弱引用方式，singleton = false 的情况下不缓存
     *
     * <p>使用GkConcurrentReferenceHashMap作为缓存容器，支持弱引用以避免内存泄漏。 当接口标记为非单例时，不会缓存其实例。
     */
    private static final Map<Class<?>, Object> entityTableMap =
            new GkConcurrentReferenceHashMap<Class<?>, Object>();

    /**
     * 根据接口类型加载其实现类实例
     *
     * <p>支持泛型类型参数，按以下顺序查找实现类： 1. 检查接口是否标记为单例，如果是则优先从缓存获取 2. 按照@GkSP注解中指定的loader顺序加载 3.
     * 如果前面都未找到，则使用默认的GirPlaceHolderSpLoader加载
     *
     * @param <T> 泛型类型
     * @param requiredType 接口类型
     * @param types 泛型类型数组
     * @return 接口实现类实例，如果找不到则返回null
     * @throws GirException 当接口未添加@GkSP注解时抛出异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(Class<T> requiredType, Type... types) {

        GkSP spiAn = requiredType.getAnnotation(GkSP.class);
        if (spiAn == null) {
            throw new GirException("获取sp实现的接口必须包含GkSP注解：{} ", requiredType.getName());
        }

        T res = null;

        if (spiAn.singleton()) {
            res = (T) entityTableMap.get(requiredType);
        }

        if (res == null) {
            Class<? extends GkSpLoader>[] loaders = spiAn.loader();
            for (Class<? extends GkSpLoader> loader : loaders) {
                res = getGkSpiLoader(loader).load(requiredType, types);
                if (res != null) {
                    break;
                }
            }
            if (res == null) {
                res = getGkSpiLoader(GirPlaceHolderSpLoader.class).load(requiredType, types);
            }
        }
        return res;
    }

    /**
     * 根据名称和接口类型加载实现类实例（暂未实现）
     *
     * <p>TODO: 此方法尚未实现，暂时返回null
     *
     * @param <T> 泛型类型
     * @param name 实现类名称
     * @param requiredType 接口类型
     * @return 接口实现类实例
     */
    public static <T> T load(String name, Class<T> requiredType) {
        return null;
    }
}
