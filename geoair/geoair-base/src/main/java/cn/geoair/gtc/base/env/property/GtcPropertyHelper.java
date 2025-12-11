package cn.geoair.gtc.base.env.property;

import cn.geoair.gtc.base.env.property.support.GtcSystemPropertierOffice;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;

public class GtcPropertyHelper {

//	/*
//	static {
//		MethodHand.implFromClass( gtcPropertyHelper.class);
//	}
//	*/

	@GaMethodHandDefine(expectClassName = "com.gtc.spi.env.SpringEnvironment4Gtc")
	public static GiPropertier getPropertier() {
		return (GiPropertier) GkMethodHand.invokeSelf();
	}


	@GaMethodHandImpl(implClass= GtcPropertyHelper.class,implMethod="getPropertier",type= GaMethodHandImpl.ImplType.comity)
	protected GiPropertier _getPropertier() {
		return new GtcSystemPropertierOffice().getOperater();
	}

}
