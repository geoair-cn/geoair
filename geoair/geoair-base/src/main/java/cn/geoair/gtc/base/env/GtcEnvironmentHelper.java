package cn.geoair.gtc.base.env;

import cn.geoair.gtc.base.env.support.GtcSystemEnvironmentOffice;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;

public class GtcEnvironmentHelper {

	/*
	static {
		MethodHand.implFromClass( gtcEnvironmentHelper.class);
	}
	*/


	@GaMethodHandDefine(expectClassName = "com.gtc.spi.env.SpringEnvironment4Gtc")
	public static GiEnvironmenter getEnvironmenter() {
		Object o = GkMethodHand.invokeSelf();
		return (GiEnvironmenter)o;
	}


	@GaMethodHandImpl(implClass= GtcEnvironmentHelper.class,implMethod="getEnvironmenter",type= GaMethodHandImpl.ImplType.comity)
	protected GiEnvironmenter _getEnvironmenter() {
		return new GtcSystemEnvironmentOffice().getOperater();
	}

}
