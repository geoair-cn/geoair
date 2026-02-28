package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;
import cn.geoair.gtc.base.Gir;

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
