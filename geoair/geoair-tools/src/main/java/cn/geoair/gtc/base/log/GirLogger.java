package cn.geoair.gtc.base.log;


import cn.geoair.gtc.base.lang.caller.GkCallerUtil;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;

public class GirLogger {

	private GirLogger() {}

	static {
		GkMethodHand.implFromClass( GirLogger.class);
	}

	public static GiLogger getLoger(Class<?> clazz) {
		return getLoger(clazz.getName());
	}

	@GaMethodHandDefine(expectClassName = "com.gtc.spi.log.Log4Gir")
	public static GiLogger getLoger(String name) {
		return (GiLogger)GkMethodHand.invokeSelf(name);
	}


	public static GiLogger getLoger() {
		return getLoger(GkCallerUtil.getCallerName());
	}


	@GaMethodHandImpl(implClass =  GirLogger.class, implMethod = "getLoger", type = GaMethodHandImpl.ImplType.comity)
	private static GiLogger _getLoger(String name) {
		return  GirConsoleLog.forName(name);
	}

}
