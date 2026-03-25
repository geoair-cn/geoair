package cn.geoair.base.sp.support;

import cn.geoair.base.bean.GirBeanHelper;
import cn.geoair.base.bean.GirNoSuchBeanException;
import cn.geoair.base.bean.GirNoUniqueBeanException;
import cn.geoair.base.exception.GirException;
import cn.geoair.base.sp.annotation.GkSP;
import java.lang.reflect.Type;

public class GirBeanFactorySpLoader extends GirCacheSpLoader {

    @Override
    public <T> T load(Class<T> cls, Type[] types) {

        GkSP spiAn = cls.getAnnotation(GkSP.class);
        if (spiAn == null) {
            throw new GirException("获取sp实现的接口必须包含GkSP注解：{} ", cls.getName());
        }
        T t = null;
        if (spiAn.singleton()) {
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
        if (spiAn.singleton() && t != null) {
            this.setCache(cls, types, t);
        }
        return t;
    }
}
