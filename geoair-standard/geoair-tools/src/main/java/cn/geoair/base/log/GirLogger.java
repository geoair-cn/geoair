package cn.geoair.base.log;

import cn.geoair.base.lang.caller.GkCallerUtil;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;

/**
 *  因为命名上跟不上主流的slf4j，导致有歧义，故给废弃了，还请移步 GirLoggerFactory
 */
@Deprecated
public class GirLogger {

    private GirLogger() {}

    static {
        GkMethodHand.implFromClass(GirLogger.class);
    }

    public static GiLogger getLoger(Class<?> clazz) {
        return getLoger(clazz.getName());
    }

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.log.Log4Gir")
    public static GiLogger getLoger(String name) {
        return (GiLogger) GkMethodHand.invokeSelf(name);
    }

    public static GiLogger getLoger() {
        return getLoger(GkCallerUtil.getCallerCallerName());
    }

    @GaMethodHandImpl(
        implClass = GirLogger.class,
        implMethod = "getLoger",
        type = GaMethodHandImpl.ImplType.comity
    )
    private static GiLogger _getLoger(String name) {
        return GirConsoleLog.forName(name);
    }
}
