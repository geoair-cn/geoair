package cn.geoair.base.sp.support;

import cn.geoair.base.bean.GirNoUniqueBeanException;
import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.util.GutilGenericType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * ServiceLoader 提供的spLoader
 *
 * @author Ray
 */
public class GirJdkSpLoader extends GirCacheSpLoader {

    @Override
    public <T> T load(Class<T> cls, Type[] types) {

        GkSP spiAn = cls.getAnnotation(GkSP.class);
        boolean singleton = (spiAn != null) ? spiAn.singleton() : true;

        T t = null;
        if (singleton) {
            t = super.load(cls, types);
        }

        if (t == null) {
            ServiceLoader<T> loaders = ServiceLoader.load(cls);
            Iterator<T> it = loaders.iterator();
            List<T> res = new ArrayList<>();
            if (types.length > 0) {
                loop1:
                while (it.hasNext()) {
                    T spiSer = it.next();
                    Type[] genericTypes =
                            GutilGenericType.resolveTypeArguments(spiSer.getClass(), cls);
                    if (genericTypes != null && types.length == genericTypes.length) {
                        for (int i = 0; i < genericTypes.length; i++) {
                            if (genericTypes[i] != types[i]) {
                                continue loop1;
                            }
                        }
                        res.add(spiSer);
                    }
                }
            } else {
                while (it.hasNext()) {
                    T spiSer = it.next();
                    res.add(spiSer);
                }
            }
            if (res.size() == 1) {
                t = res.get(0);
            } else if (res.size() > 1) {
                throw new GirNoUniqueBeanException(" JdkSpLoader找到了不只一个配置适配" + cls.getName());
            }
        }
        if (singleton && t != null) {
            this.setCache(cls, types, t);
        }
        return t;
    }

    @Override
    public <T> T load(Class<T> cls, String name, Type[] types) {

        GkSP spiAn = cls.getAnnotation(GkSP.class);
        boolean singleton = (spiAn != null) ? spiAn.singleton() : true;

        if (name == null || name.isEmpty()) {
            return null;
        }

        T t = null;
        if (singleton) {
            t = super.load(cls, name, types);
        }

        if (t == null) {
            ServiceLoader<T> loaders = ServiceLoader.load(cls);
            Iterator<T> it = loaders.iterator();
            List<T> matched = new ArrayList<>();
            while (it.hasNext()) {
                T spiSer = it.next();
                // 通过实现类的Class简单名称匹配name
                String simpleName = spiSer.getClass().getSimpleName();
                if (name.equals(simpleName)) {
                    // 如果指定了泛型，还需验证泛型匹配
                    if (types.length > 0) {
                        Type[] genericTypes =
                                GutilGenericType.resolveTypeArguments(spiSer.getClass(), cls);
                        if (genericTypes != null && types.length == genericTypes.length) {
                            boolean typeMatch = true;
                            for (int i = 0; i < genericTypes.length; i++) {
                                if (genericTypes[i] != types[i]) {
                                    typeMatch = false;
                                    break;
                                }
                            }
                            if (typeMatch) {
                                matched.add(spiSer);
                            }
                        }
                    } else {
                        matched.add(spiSer);
                    }
                }
            }
            if (matched.size() == 1) {
                t = matched.get(0);
            } else if (matched.size() > 1) {
                throw new GirNoUniqueBeanException(
                        " JdkSpLoader根据name[" + name + "]找到了不只一个配置适配" + cls.getName());
            }
        }

        if (singleton && t != null) {
            this.setCache(cls, name, types, t);
        }
        return t;
    }

    @Override
    public <T> List<T> loadAll(Class<T> cls, Type[] types) {

        List<T> result = new ArrayList<>();
        try {
            ServiceLoader<T> loaders = ServiceLoader.load(cls);
            Iterator<T> it = loaders.iterator();
            while (it.hasNext()) {
                T spiSer = it.next();
                if (types.length > 0) {
                    Type[] genericTypes =
                            GutilGenericType.resolveTypeArguments(spiSer.getClass(), cls);
                    if (genericTypes != null && types.length == genericTypes.length) {
                        boolean typeMatch = true;
                        for (int i = 0; i < genericTypes.length; i++) {
                            if (genericTypes[i] != types[i]) {
                                typeMatch = false;
                                break;
                            }
                        }
                        if (typeMatch) {
                            result.add(spiSer);
                        }
                    }
                } else {
                    result.add(spiSer);
                }
            }
        } catch (Exception e) {
            //
        }
        return result;
    }
}
