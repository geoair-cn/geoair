package cn.geoair.base.log;

import cn.geoair.base.lang.caller.GkCallerUtil;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;

public class GirLogger {

	private GirLogger() {
	}

	static {
		GkMethodHand.implFromClass(GirLogger.class);
	}

	public static GiLogger getLoger(Class<?> clazz) {
		return getLoger(clazz.getName());
	}

	@GaMethodHandDefine(expectClassName = "cn.geoair.gtc.spi.log.Log4Gir")
	public static GiLogger getLoger(String name) {
		return (GiLogger) GkMethodHand.invokeSelf(name);
	}

	public static GiLogger getLoger() {
		return getLoger(GkCallerUtil.getCallerName());
	}

	@GaMethodHandImpl(implClass = GirLogger.class, implMethod = "getLoger", type = GaMethodHandImpl.ImplType.comity)
	private static GiLogger _getLoger(String name) {
		return GirConsoleLog.forName(name);
	}

}
