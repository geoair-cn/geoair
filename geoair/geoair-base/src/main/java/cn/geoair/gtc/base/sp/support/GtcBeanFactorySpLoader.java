package cn.geoair.gtc.base.sp.support;

import java.lang.reflect.Type;

import cn.geoair.gtc.base.bean.GtcBeanHelper;
import cn.geoair.gtc.base.bean.GtcNoSuchBeanException;
import cn.geoair.gtc.base.bean.GtcNoUniqueBeanException;
import cn.geoair.gtc.base.exception.GtcException;
import cn.geoair.gtc.base.sp.annotation.GkSP;

public class GtcBeanFactorySpLoader extends GtcCacheSpLoader {

	@Override
	public <T> T load(Class<T> cls,Type[] types) {

		GkSP spiAn = cls.getAnnotation(GkSP.class);
    	if(spiAn == null) {
    		throw new GtcException("获取sp实现的接口必须包含GkSP注解：{} ",cls.getName());
    	}
    	T t = null;
		if(spiAn.singleton()) {
			t = super.load(cls, types);
		}
		if(t == null) {
			if( GtcBeanHelper.getProvider() != null) {
				try {
					if(types.length > 0) {
						t =  GtcBeanHelper.getProvider().getBean(cls, types);
					}else {
						t =  GtcBeanHelper.getProvider().getBean(cls);
					}
				}catch( GtcNoUniqueBeanException nex) {
					throw nex;
				}catch( GtcNoSuchBeanException nex) {
					//
				}
			}
		}
		if(spiAn.singleton() && t != null) {
			this.setCache(cls, types, t);
		}
		return t;
	}

}
