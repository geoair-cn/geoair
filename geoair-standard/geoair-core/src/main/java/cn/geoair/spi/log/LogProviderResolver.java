package cn.geoair.spi.log;

import cn.geoair.base.util.GutilClass;

final class LogProviderResolver {

    private LogProviderResolver() {}

    static Log4Gir.LogType resolve() {
        ClassLoader classLoader = Log4Gir.class.getClassLoader();
        if (GutilClass.isPresent("cn.hutool.log.LogFactory", classLoader)) {
            return Log4Gir.LogType.HUTOOL;
        } else if (GutilClass.isPresent("org.slf4j.LoggerFactory", classLoader)) {
            return Log4Gir.LogType.SLF4J;
        } else if (GutilClass.isPresent("org.apache.commons.logging.LogFactory", classLoader)) {
            return Log4Gir.LogType.APPACHECOMMONS;
        }
        return Log4Gir.LogType.CONSOLE;
    }
}
