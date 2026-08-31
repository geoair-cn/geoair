package cn.geoair.spi.test;

import cn.geoair.base.log.GiLogger;
import cn.geoair.spi.log.Log4Gir;

/** Log4Gir 入口示例 */
public class Log4GirExample {

    public static void main(String[] args) {
        Log4Gir.setLogType(Log4Gir.LogType.CONSOLE);
        GiLogger logger = Log4Gir.getLogger("geoair-core-demo");

        logger.info("hello {}, version={}", "geoair", "1.0");
        logger.warn("warn message");
        logger.error("error message");

        System.out.println("logger class = " + logger.getClass().getName());
    }
}
