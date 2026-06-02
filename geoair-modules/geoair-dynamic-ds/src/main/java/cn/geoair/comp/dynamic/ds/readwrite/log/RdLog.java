package cn.geoair.comp.dynamic.ds.readwrite.log;

import cn.geoair.base.log.GemLogLevel;
import cn.geoair.base.log.GirLogWrapper;

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


    protected void recordLog(GemLogLevel level, String message, LoggerInfo loggerInfo) {
        if (minLogLevel.isGreaterOrEqual(level)) {
            super.recordLog(level, message, loggerInfo);
        }
    }


    protected void recordLogWithThrowable(GemLogLevel level, String message, Throwable t, LoggerInfo loggerInfo) {
        if (level.isGreaterOrEqual(minLogLevel)) {
            super.recordLog(level, message, loggerInfo);
        }
    }
}
