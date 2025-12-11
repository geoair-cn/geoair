package cn.geoair.gtc.base.log;


import cn.geoair.gtc.base.lang.caller.GkCallerUtil;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;

public class GtcLoger {

	private GtcLoger() {}

	static {
		GkMethodHand.implFromClass( GtcLoger.class);
	}

	public static GiLoger getLoger(Class<?> clazz) {
		return getLoger(clazz.getName());
	}

	@GaMethodHandDefine(expectClassName = "com.gtc.spi.log.Log4Gtc")
	public static GiLoger getLoger(String name) {
		return (GiLoger)GkMethodHand.invokeSelf(name);
	}


	public static GiLoger getLoger() {
		return getLoger(GkCallerUtil.getCallerName());
	}


	@GaMethodHandImpl(implClass =  GtcLoger.class, implMethod = "getLoger", type = GaMethodHandImpl.ImplType.comity)
	private static GiLoger _getLoger(String name) {
		return  GtcConsoleLog.forName(name);
	}

}
