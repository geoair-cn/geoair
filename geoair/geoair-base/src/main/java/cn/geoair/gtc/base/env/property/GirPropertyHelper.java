package cn.geoair.gtc.base.env.property;

import cn.geoair.gtc.base.env.property.support.GirSystemPropertierOffice;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;

public class GirPropertyHelper {

//	/*
//	static {
//		MethodHand.implFromClass( gtcPropertyHelper.class);
//	}
//	*/

	@GaMethodHandDefine(expectClassName = "cn.geoair.gtc.spi.env.SpringEnvironment4Gir")
	public static GiPropertier getPropertier() {
		return (GiPropertier) GkMethodHand.invokeSelf();
	}


	@GaMethodHandImpl(implClass= GirPropertyHelper.class,implMethod="getPropertier",type= GaMethodHandImpl.ImplType.comity)
	protected GiPropertier _getPropertier() {
		return new GirSystemPropertierOffice().getOperater();
	}

}
