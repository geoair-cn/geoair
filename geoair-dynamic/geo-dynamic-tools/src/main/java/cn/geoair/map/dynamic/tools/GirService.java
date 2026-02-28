package cn.geoair.map.dynamic.tools;

import cn.geoair.gtc.base.Gir;
import cn.hutool.core.lang.Singleton;
import cn.hutool.extra.spring.SpringUtil;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/9/8 14:18 @description： 静态获取spring托管的Bean的方法，并进行缓存
 */
public class GirService {

	public static <T> T getPxyBeanC(Class<T> classs) {
		if (Singleton.exists(classs)) {
			return Singleton.get(classs);
		}
		else {
			T bean = Gir.beans.getBean(classs);
			Singleton.put(classs.getName(), bean);
			return bean;
		}

	}

}
