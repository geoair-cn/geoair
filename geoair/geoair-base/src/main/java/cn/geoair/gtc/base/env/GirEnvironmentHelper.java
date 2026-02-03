package cn.geoair.gtc.base.env;

import cn.geoair.gtc.base.env.support.GirSystemEnvironmentOffice;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;

public class GirEnvironmentHelper {

	/*
	static {
		MethodHand.implFromClass( gtcEnvironmentHelper.class);
	}
	*/


	@GaMethodHandDefine(expectClassName = "cn.geoair.gtc.spi.env.SpringEnvironment4Gir")
	public static GiEnvironmenter getEnvironmenter() {
		Object o = GkMethodHand.invokeSelf();
		return (GiEnvironmenter)o;
	}


	@GaMethodHandImpl(implClass= GirEnvironmentHelper.class,implMethod="getEnvironmenter",type= GaMethodHandImpl.ImplType.comity)
	protected GiEnvironmenter _getEnvironmenter() {
		return new GirSystemEnvironmentOffice().getOperater();
	}

}
