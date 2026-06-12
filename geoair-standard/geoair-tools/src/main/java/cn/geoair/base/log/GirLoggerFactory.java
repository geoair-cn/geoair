package cn.geoair.base.log;

import cn.geoair.base.lang.caller.GkCallerUtil;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/2 09:26
 * @description： 方法名称对齐Slf4j
 */
public class GirLoggerFactory {

    private GirLoggerFactory() {}

    static {
        GkMethodHand.implFromClass(GirLoggerFactory.class);
    }

    public static GiLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.log.Log4Gir")
    public static GiLogger getLogger(String name) {
        return (GiLogger) GkMethodHand.invokeSelf(name);
    }

    public static GiLogger getLogger() {
        return getLogger(GkCallerUtil.getCallerCallerName());
    }

    @GaMethodHandImpl(
            implClass = GirLoggerFactory.class,
            implMethod = "getLogger",
            type = GaMethodHandImpl.ImplType.comity)
    private static GiLogger _getLogger(String name) {
        return GirConsoleLog.forName(name);
    }
}
