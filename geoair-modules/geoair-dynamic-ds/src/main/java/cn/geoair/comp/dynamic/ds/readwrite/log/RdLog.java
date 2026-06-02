package cn.geoair.comp.dynamic.ds.readwrite.log;

import cn.geoair.base.log.GemLogLevel;
import cn.geoair.base.log.GirLogWrapper;
import cn.hutool.log.dialect.console.ConsoleColorLogFactory;
import cn.hutool.log.level.Level;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/2 09:59
 * @description： 读写分离模块的日志代理实现
 */
public class RdLog extends GirLogWrapper {

    static RdLog INSTANCE = new RdLog();

    public static RdLog getInstance() {
        return INSTANCE;
    }


    /**
     * 输出的最小的日志级别 , 这里只是标记输出的最小级别，
     * 这只是第一道拦截器，具体的日志级别还需要看具体的日志实现
     */
    public static GemLogLevel minLogLevel = GemLogLevel.DEBUG;

    /**
     * 是否使用独立日志实现，不依托于全局的日志实现
     * <p>
     * true: 使用独立的日志实现（如自定义的日志处理逻辑）<br>
     * false: 使用默认的日志实现（如 Slf4j、Log4j 等）
     */
    public static boolean useIndependentLog = false;

    ConsoleColorLogFactory consoleColorLogFactory = null;

    protected void recordLog(GemLogLevel level, String message, LoggerInfo loggerInfo, Object... arguments) {
        if (level.isGreaterOrEqual(minLogLevel)) {
            if (useIndependentLog) {
                if (consoleColorLogFactory == null) {
                    consoleColorLogFactory = new ConsoleColorLogFactory();
                }
                consoleColorLogFactory.createLog(loggerInfo.getClassName()).log(Level.valueOf(level.name()), message);
            } else {
                super.recordLog(level, message, loggerInfo);
            }

        }
    }


    protected void recordLogWithThrowable(GemLogLevel level, String message, Throwable t, LoggerInfo loggerInfo, Object... arguments) {
        if (level.isGreaterOrEqual(minLogLevel)) {
            if (useIndependentLog) {
                if (consoleColorLogFactory == null) {
                    consoleColorLogFactory = new ConsoleColorLogFactory();
                }
                consoleColorLogFactory.createLog(loggerInfo.getClassName()).log(Level.valueOf(level.name()), t, message);
            } else {
                super.recordLogWithThrowable(level, message, t, loggerInfo);
            }
        }
    }
}
