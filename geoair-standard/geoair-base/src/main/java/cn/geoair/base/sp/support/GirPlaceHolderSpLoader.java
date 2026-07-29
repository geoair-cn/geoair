package cn.geoair.base.sp.support;

import cn.geoair.base.bean.GirNoUniqueBeanException;
import cn.geoair.base.exception.GirException;
import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.util.GutilArray;
import cn.geoair.base.util.GutilClass;
import cn.geoair.base.util.GutilGenericType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class GirPlaceHolderSpLoader extends GirCacheSpLoader {

    /** placeHolder 缓存 */
    private static final Map<Class<?>, Class<?>[]> PLACEHOLDERCLASS_CACHE =
            new ConcurrentHashMap<Class<?>, Class<?>[]>();

    private static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * 获取接口的默认实现类 不带泛型区分
     *
     * <p>如果接口未添加@GkSP注解，返回空数组
     *
     * @param <T>
     * @param requiredType
     * @return
     */
    public static <T> Class<? extends T>[] getPlaceHolderClasses(Class<T> requiredType) {
        GkSP spiAn = requiredType.getAnnotation(GkSP.class);
        if (spiAn == null) {
            return (Class<? extends T>[]) new Class<?>[0];
        }
        Class<?>[] ts = null;
        if (PLACEHOLDERCLASS_CACHE.containsKey(requiredType)) {
            ts = PLACEHOLDERCLASS_CACHE.get(requiredType);
        } else {
            LOCK.lock();
            try {
                ts = spiAn.placeHolderClass();
                for (Class<?> t : ts) {
                    if (!requiredType.isAssignableFrom(t)) {
                        throw new GirException(
                                "类 {} 配置sp 默认placeHolderClass实现类 ：{} 类型不匹配",
                                requiredType.getName(),
                                t.getName());
                    }
                }
                String[] clsNames = spiAn.placeHolder();
                if (clsNames.length > 0) {
                    Class<?>[] cls2 = new Class<?>[clsNames.length];
                    for (int i = 0; i < clsNames.length; i++) {
                        try {
                            cls2[i] = GutilClass.forName(clsNames[i], null);
                        } catch (Exception e) {
                            throw new GirException(
                                    e,
                                    "类 {} 配置sp 默认placeHolder实现类 ：{} 未找到",
                                    requiredType.getName(),
                                    clsNames[i]);
                        }
                        if (!requiredType.isAssignableFrom(cls2[i])) {
                            throw new GirException(
                                    "类 {} 配置sp 默认placeHolder实现类 ：{} 类型不匹配",
                                    requiredType.getName(),
                                    clsNames[i]);
                        }
                    }
                    ts = GutilArray.addAll(cls2, spiAn.placeHolderClass());
                }
                PLACEHOLDERCLASS_CACHE.put(requiredType, ts);

            } finally {
                LOCK.unlock();
            }
        }
        return (Class<? extends T>[]) ts;
    }

    /** placeHolder Type 缓存 */
    private static final Map<String, Class<?>[]> PLACEHOLDERCLASS_TYPE_CACHE =
            new ConcurrentHashMap<String, Class<?>[]>();

    private static final ReentrantLock LOCK_TYPE = new ReentrantLock();

    /**
     * 获取接口的默认实现类,带泛型区分
     *
     * <p>如果接口未添加@GkSP注解，返回空数组
     *
     * @param <T>
     * @param requiredType
     * @param types 泛型
     * @return
     */
    public static <T> Class<? extends T>[] getPlaceHolderClasses(
            Class<T> requiredType, Type[] types) {

        String cachekey = strKeyForClassAndTypes(requiredType, types);

        Class<?>[] res = null;
        if (PLACEHOLDERCLASS_TYPE_CACHE.containsKey(cachekey)) {
            res = PLACEHOLDERCLASS_TYPE_CACHE.get(cachekey);
        } else {
            LOCK_TYPE.lock();
            try {
                Class<? extends T>[] ts = getPlaceHolderClasses(requiredType);
                res = new Class<?>[0];
                loop1:
                for (Class<?> t : ts) {
                    Type[] genericTypes = GutilGenericType.resolveTypeArguments(t, requiredType);
                    if (genericTypes == null) {
                        if (types.length == 0) {
                            res = GutilArray.append(res, t);
                        }
                    } else if (types.length == genericTypes.length) {
                        for (int i = 0; i < genericTypes.length; i++) {
                            if (genericTypes[i] != types[i]) {
                                continue loop1;
                            }
                        }
                        res = GutilArray.append(res, t);
                    }
                }
                PLACEHOLDERCLASS_TYPE_CACHE.put(cachekey, res);

            } finally {
                LOCK_TYPE.unlock();
            }
        }
        return (Class<? extends T>[]) res;
    }

    @Override
    public <T> T load(Class<T> requiredType, Type[] types) {

        GkSP spiAn = requiredType.getAnnotation(GkSP.class);
        boolean singleton = (spiAn != null) ? spiAn.singleton() : true;

        T res = null;
        if (singleton) {
            res = super.load(requiredType, types);
        }
        if (res == null) {
            Class<? extends T>[] phClzs = getPlaceHolderClasses(requiredType, types);
            int len = phClzs.length;
            if (len > 0) {
                if (phClzs.length != 1) {
                    throw new GirNoUniqueBeanException(
                            "类 {} 配置sp 默认placeHolder发现多个实现类", requiredType.getName());
                }
                Class<? extends T> cls = phClzs[0];
                try {
                    res = cls.newInstance();
                } catch (Exception e) {
                    throw new GirException(
                            e,
                            "类 {} 配置sp 默认placeHolder实现类 ：{} 实例化对象发生错误",
                            requiredType.getName(),
                            cls.getName());
                }
            }
        }
        if (res != null) {
            if (singleton) {
                setCache(requiredType, types, res);
            }
        }
        return res;
    }

    @Override
    public <T> T load(Class<T> requiredType, String name, Type[] types) {

        GkSP spiAn = requiredType.getAnnotation(GkSP.class);
        boolean singleton = (spiAn != null) ? spiAn.singleton() : true;

        if (name == null || name.isEmpty()) {
            return null;
        }

        T res = null;
        if (singleton) {
            res = super.load(requiredType, name, types);
        }
        if (res == null) {
            Class<? extends T>[] phClzs = getPlaceHolderClasses(requiredType, types);
            List<Class<? extends T>> matched = new ArrayList<>();
            for (Class<? extends T> cls : phClzs) {
                // 通过实现类的Class简单名称匹配name
                if (name.equals(cls.getSimpleName())) {
                    matched.add(cls);
                }
            }
            if (matched.size() == 1) {
                try {
                    res = matched.get(0).newInstance();
                } catch (Exception e) {
                    throw new GirException(
                            e,
                            "类 {} 配置sp 默认placeHolder实现类 ：{} 实例化对象发生错误",
                            requiredType.getName(),
                            matched.get(0).getName());
                }
            } else if (matched.size() > 1) {
                throw new GirNoUniqueBeanException(
                        "类 {} 配置sp 默认placeHolder根据name[{}]发现多个实现类",
                        requiredType.getName(),
                        name);
            }
        }
        if (res != null && singleton) {
            setCache(requiredType, name, types, res);
        }
        return res;
    }

    @Override
    public <T> List<T> loadAll(Class<T> requiredType, Type[] types) {

        List<T> result = new ArrayList<>();
        Class<? extends T>[] phClzs = getPlaceHolderClasses(requiredType, types);
        for (Class<? extends T> cls : phClzs) {
            try {
                result.add(cls.newInstance());
            } catch (Exception e) {
                throw new GirException(
                        e,
                        "类 {} 配置sp 默认placeHolder实现类 ：{} 实例化对象发生错误",
                        requiredType.getName(),
                        cls.getName());
            }
        }
        return result;
    }
}
