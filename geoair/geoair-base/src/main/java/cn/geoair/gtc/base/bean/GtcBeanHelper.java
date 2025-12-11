package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;
import cn.geoair.gtc.base.Gtc;

public class GtcBeanHelper {

	private GtcBeanHelper() {

	}

	static {
		GkMethodHand.implFromClass( GtcBeanHelper.class);
	}

	@GaMethodHandDefine(expectClassName = "com.gtc.spi.bean.SpringContextBean4Gtc")
	public static GiBeanFactory getProvider() {
		return (GiBeanFactory)GkMethodHand.invokeSelf();
	}


	@GaMethodHandImpl(implClass= GtcBeanHelper.class,implMethod="getProvider",type= GaMethodHandImpl.ImplType.comity)
	private static GiBeanFactory _getProvider() {
		 Gtc.log.error("必须有工具提供容器，如spring");
		return null;
	}

}
