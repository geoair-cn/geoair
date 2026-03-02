package cn.geoair.base.bean;

import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.base.Gir;

public class GirBeanHelper {

	private GirBeanHelper() {

	}

	static {
		GkMethodHand.implFromClass(GirBeanHelper.class);
	}

	@GaMethodHandDefine(expectClassName = "cn.geoair.gtc.spi.bean.SpringContextBean4Gir")
	public static GiBeanFactory getProvider() {
		return (GiBeanFactory) GkMethodHand.invokeSelf();
	}

	@GaMethodHandImpl(implClass = GirBeanHelper.class, implMethod = "getProvider",
			type = GaMethodHandImpl.ImplType.comity)
	private static GiBeanFactory _getProvider() {
		Gir.log.error("必须有工具提供容器，如spring");
		return null;
	}

}
