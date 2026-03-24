package cn.geoair.base.sp.support;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import cn.geoair.base.bean.GirNoUniqueBeanException;
import cn.geoair.base.exception.GirException;
import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.util.GutilGenericType;

/**
 * ServiceLoader 提供的spLoader
 *
 * @author Ray
 *
 */
public class GirJdkSpLoader extends GirCacheSpLoader {

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
			ServiceLoader<T> loaders = ServiceLoader.load(cls);
			Iterator<T> it = loaders.iterator();
			List<T> res = new ArrayList<>();
			if (types.length > 0) {
				loop1: while (it.hasNext()) {
					T spiSer = it.next();
					Type[] genericTypes = GutilGenericType.resolveTypeArguments(spiSer.getClass(), cls);
					if (genericTypes != null && types.length == genericTypes.length) {
						for (int i = 0; i < genericTypes.length; i++) {
							if (genericTypes[i] != types[i]) {
								continue loop1;
							}
						}
						res.add(spiSer);
					}
				}
			}
			else {
				while (it.hasNext()) {
					T spiSer = it.next();
					res.add(spiSer);
				}
			}
			if (res.size() == 1) {
				t = res.get(0);
			}
			else if (res.size() > 1) {
				throw new GirNoUniqueBeanException(" JdkSpLoader找到了不只一个配置适配" + cls.getName());
			}
		}
		if (spiAn.singleton() && t != null) {
			this.setCache(cls, types, t);
		}
		return t;
	}

}
