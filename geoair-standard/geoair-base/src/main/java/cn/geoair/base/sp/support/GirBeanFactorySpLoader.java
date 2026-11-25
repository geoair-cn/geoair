package cn.geoair.base.sp.support;

import cn.geoair.base.bean.GirBeanHelper;
import cn.geoair.base.bean.GirNoSuchBeanException;
import cn.geoair.base.bean.GirNoUniqueBeanException;
import cn.geoair.base.sp.annotation.GkSP;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GirBeanFactorySpLoader extends GirCacheSpLoader {

    @Override
    public <T> T load(Class<T> cls, Type[] types) {

        GkSP spiAn = cls.getAnnotation(GkSP.class);
        boolean singleton = (spiAn != null) ? spiAn.singleton() : true;

        T t = null;
        if (singleton) {
            t = super.load(cls, types);
        }
        if (t == null) {
            if (GirBeanHelper.getProvider() != null) {
                try {
                    if (types.length > 0) {
                        t = GirBeanHelper.getProvider().getBean(cls, types);
                    } else {
                        t = GirBeanHelper.getProvider().getBean(cls);
                    }
                } catch (GirNoUniqueBeanException nex) {
                    throw nex;
                } catch (GirNoSuchBeanException nex) {
                    //
                }
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

        T t = null;
        if (singleton) {
            t = super.load(cls, name, types);
        }
        if (t == null) {
            if (GirBeanHelper.getProvider() != null) {
                try {
                    t = GirBeanHelper.getProvider().getBean(name, cls);
                } catch (GirNoSuchBeanException nex) {
                    //
                }
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
        if (GirBeanHelper.getProvider() != null) {
            try {
                Map<String, T> beansMap;
                if (types.length > 0) {
                    beansMap = GirBeanHelper.getProvider().getBeans(cls, types);
                } else {
                    beansMap = GirBeanHelper.getProvider().getBeans(cls);
                }
                if (beansMap != null && !beansMap.isEmpty()) {
                    result.addAll(beansMap.values());
                }
            } catch (Exception e) {
                //
            }
        }
        return result;
    }
}
