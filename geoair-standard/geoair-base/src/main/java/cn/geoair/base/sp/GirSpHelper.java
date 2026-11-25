package cn.geoair.base.sp;

import cn.geoair.base.exception.GirException;
import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.sp.support.GirBeanFactorySpLoader;
import cn.geoair.base.sp.support.GirJdkSpLoader;
import cn.geoair.base.sp.support.GirPlaceHolderSpLoader;
import cn.geoair.base.tool.GkConcurrentReferenceHashMap;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service Provider 工具类
 *
 * <p>提供服务发现与加载功能，支持基于注解的SPI机制，可以根据接口类型动态加载其实现类。 支持单例模式缓存，避免重复创建实例，提高系统性能。
 *
 * <p>接口可以使用{@link GkSP @GkSP}注解配置加载策略，也可以不使用注解直接调用， 此时将使用默认配置：
 *
 * <ul>
 *   <li>加载器链：GirBeanFactorySpLoader → GirJdkSpLoader
 *   <li>单例模式：true
 *   <li>默认实现类：无
 * </ul>
 *
 * @author Ray
 */
public class GirSpHelper {

    /** 私有构造函数，防止外部实例化 */
    private GirSpHelper() {}

    /** 默认加载器链 */
    @SuppressWarnings("unchecked")
    private static final Class<? extends GkSpLoader>[] DEFAULT_LOADERS =
            new Class[] {GirBeanFactorySpLoader.class, GirJdkSpLoader.class};

    /**
     * 获取加载器链配置，注解缺失时使用默认值
     *
     * @param spiAn @GkSP注解实例，可能为null
     * @return 加载器链
     */
    private static Class<? extends GkSpLoader>[] getLoaders(GkSP spiAn) {
        if (spiAn != null) {
            return spiAn.loader();
        }
        return DEFAULT_LOADERS;
    }

    /**
     * 获取单例配置，注解缺失时默认为true
     *
     * @param spiAn @GkSP注解实例，可能为null
     * @return 是否单例
     */
    private static boolean isSingleton(GkSP spiAn) {
        if (spiAn != null) {
            return spiAn.singleton();
        }
        return true;
    }

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
     * <p>如果接口未添加@GkSP注解，将使用默认配置：加载器链为GirBeanFactorySpLoader→GirJdkSpLoader，单例模式为true。
     *
     * @param <T> 泛型类型
     * @param requiredType 接口类型
     * @param types 泛型类型数组
     * @return 接口实现类实例，如果找不到则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(Class<T> requiredType, Type... types) {

        GkSP spiAn = requiredType.getAnnotation(GkSP.class);

        T res = null;

        if (isSingleton(spiAn)) {
            res = (T) entityTableMap.get(requiredType);
        }

        if (res == null) {
            Class<? extends GkSpLoader>[] loaders = getLoaders(spiAn);
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
     * 根据名称和接口类型加载实现类实例
     *
     * <p>在多个实现类中通过name确定唯一实例，按以下顺序查找： 1. 按照@GkSP注解中指定的loader顺序加载（通过name匹配） 2.
     * 如果前面都未找到，则使用默认的GirPlaceHolderSpLoader加载
     *
     * <p>name匹配规则：
     *
     * <ul>
     *   <li>Spring容器：通过bean name获取
     *   <li>JDK SPI：通过实现类的Class简单名称匹配
     *   <li>PlaceHolder：通过配置的Class简单名称匹配
     * </ul>
     *
     * <p>如果接口未添加@GkSP注解，将使用默认配置：加载器链为GirBeanFactorySpLoader→GirJdkSpLoader。
     *
     * @param <T> 泛型类型
     * @param name 实例名
     * @param requiredType 接口类型
     * @return 接口实现类实例，如果找不到则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(String name, Class<T> requiredType) {

        GkSP spiAn = requiredType.getAnnotation(GkSP.class);

        T res = null;
        Class<? extends GkSpLoader>[] loaders = getLoaders(spiAn);
        for (Class<? extends GkSpLoader> loader : loaders) {
            res = getGkSpiLoader(loader).load(requiredType, name, new Type[0]);
            if (res != null) {
                break;
            }
        }
        if (res == null) {
            res =
                    getGkSpiLoader(GirPlaceHolderSpLoader.class)
                            .load(requiredType, name, new Type[0]);
        }
        return res;
    }

    /**
     * 根据接口类型加载所有实现类实例
     *
     * <p>从所有加载策略中聚合实现类实例，适用于插件/扩展发现场景。 会按loader顺序依次收集，并自动去重（基于实例引用）。
     *
     * <p>注意：此方法不使用单例缓存，因为聚合场景下通常需要发现所有实现，而非获取单一实例。
     *
     * <p>如果接口未添加@GkSP注解，将使用默认配置：加载器链为GirBeanFactorySpLoader→GirJdkSpLoader。
     *
     * @param <T> 泛型类型
     * @param requiredType 接口类型
     * @param types 泛型类型数组
     * @return 所有匹配的实现类实例列表，如果找不到则返回空列表
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> loadAll(Class<T> requiredType, Type... types) {

        GkSP spiAn = requiredType.getAnnotation(GkSP.class);

        Set<T> seen = new HashSet<>();
        List<T> result = new ArrayList<>();

        Class<? extends GkSpLoader>[] loaders = getLoaders(spiAn);
        for (Class<? extends GkSpLoader> loader : loaders) {
            List<T> found = getGkSpiLoader(loader).loadAll(requiredType, types);
            if (found != null) {
                for (T item : found) {
                    if (seen.add(item)) {
                        result.add(item);
                    }
                }
            }
        }

        // PlaceHolder兜底
        List<T> phFound = getGkSpiLoader(GirPlaceHolderSpLoader.class).loadAll(requiredType, types);
        if (phFound != null) {
            for (T item : phFound) {
                if (seen.add(item)) {
                    result.add(item);
                }
            }
        }

        return result;
    }
}
