package cn.geoair.base.log;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：张俊
 * @date ：Created in 2025/9/25 14:34
 * @description： 日志代理
 */
public abstract class GirLogWrapper implements GiLogger {

    // Logger缓存（key=类名，value=对应Logger实例）
    private final Map<String, GiLogger> loggerCache = new ConcurrentHashMap<>();

    /**
     * 带缓存的动态Logger获取，并提取调用位置信息
     *
     * @return 目标Logger实例
     */
    protected LoggerInfo getTargetLoggerInfo() {
        // 获取调用栈信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        StackTraceElement callerElement = stackTrace[3];
        String callerClassName = callerElement.getClassName();
        int lineNumber = callerElement.getLineNumber();
        String fileName = callerElement.getFileName();

        // 从缓存获取或创建Logger
        GiLogger logger = loggerCache.computeIfAbsent(callerClassName, GirLoggerFactory::getLogger);

        // 返回Logger及调用位置信息
        return new LoggerInfo(logger, callerClassName, fileName, lineNumber);
    }

    /**
     * 带缓存的动态Logger获取，并提取调用位置信息
     *
     * @return 目标Logger实例
     */
    protected LoggerInfo getTargetLoggerInfo(String callerClassName) {
        // 从缓存获取或创建Logger
        GiLogger logger = loggerCache.computeIfAbsent(callerClassName, GirLoggerFactory::getLogger);
        // 返回Logger及调用位置信息
        return new LoggerInfo(logger, callerClassName, "", 0);
    }

    // 内部类用于封装Logger及调用位置信息
    protected static class LoggerInfo {
        final GiLogger logger;
        final String className;
        final String fileName;
        final int lineNumber;

        LoggerInfo(GiLogger logger, String className, String fileName, int lineNumber) {
            this.logger = logger;
            this.className = className;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }

        public GiLogger getLogger() {
            return logger;
        }

        public String getClassName() {
            return className;
        }

        public String getFileName() {
            return fileName;
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }

    /**
     * 记录日志的抽象方法，由子类实现具体的日志记录逻辑
     *
     * @param level 日志级别
     * @param message 日志消息
     * @param loggerInfo Logger信息（包含调用位置等）
     */
    protected void recordLog(
            GemLogLevel level, String message, LoggerInfo loggerInfo, Object... arguments) {
        if (level.equals(GemLogLevel.TRACE)) {
            loggerInfo.logger.trace(message, arguments);
        }
        if (level.equals(GemLogLevel.DEBUG)) {
            loggerInfo.logger.debug(message, arguments);
        }
        if (level.equals(GemLogLevel.INFO)) {
            loggerInfo.logger.info(message, arguments);
        }
        if (level.equals(GemLogLevel.WARN)) {
            loggerInfo.logger.warn(message, arguments);
        }
        if (level.equals(GemLogLevel.ERROR)) {
            loggerInfo.logger.error(message, arguments);
        }
        if (level.equals(GemLogLevel.FATAL)) {
            loggerInfo.logger.fatal(message, arguments);
        }
    }

    /**
     * 记录带异常的日志的抽象方法，由子类实现具体的日志记录逻辑
     *
     * @param level 日志级别
     * @param message 日志消息
     * @param t 异常信息
     * @param loggerInfo Logger信息（包含调用位置等）
     */
    protected void recordLogWithThrowable(
            GemLogLevel level,
            String message,
            Throwable t,
            LoggerInfo loggerInfo,
            Object... arguments) {
        if (level.equals(GemLogLevel.TRACE)) {
            loggerInfo.logger.trace(t, message, arguments);
        }
        if (level.equals(GemLogLevel.DEBUG)) {
            loggerInfo.logger.debug(t, message, arguments);
        }
        if (level.equals(GemLogLevel.INFO)) {
            loggerInfo.logger.info(t, message, arguments);
        }
        if (level.equals(GemLogLevel.WARN)) {
            loggerInfo.logger.warn(t, message, arguments);
        }
        if (level.equals(GemLogLevel.ERROR)) {
            loggerInfo.logger.error(t, message, arguments);
        }
        if (level.equals(GemLogLevel.FATAL)) {
            loggerInfo.logger.fatal(t, message, arguments);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return getTargetLoggerInfo().logger.isTraceEnabled();
    }

    @Override
    public void trace(String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        if (info.logger.isTraceEnabled()) {
            recordLog(GemLogLevel.TRACE, format, info, arguments);
        }
    }

    @Override
    public void trace(Throwable t) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.TRACE, "", t, info);
    }

    @Override
    public void trace(Throwable t, String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.TRACE, format, t, info, arguments);
    }

    @Override
    public boolean isDebugEnabled() {
        return getTargetLoggerInfo().logger.isDebugEnabled();
    }

    @Override
    public void debug(String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLog(GemLogLevel.DEBUG, format, info, arguments);
    }

    @Override
    public void debug(Throwable t) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.DEBUG, "", t, info);
    }

    @Override
    public void debug(Throwable t, String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.DEBUG, format, t, info, arguments);
    }

    @Override
    public boolean isInfoEnabled() {
        return getTargetLoggerInfo().logger.isInfoEnabled();
    }

    @Override
    public void info(String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLog(GemLogLevel.INFO, format, info, arguments);
    }

    @Override
    public void info(Throwable t) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.INFO, "", t, info);
    }

    @Override
    public void info(Throwable t, String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.INFO, format, t, info, arguments);
    }

    @Override
    public boolean isWarnEnabled() {
        return getTargetLoggerInfo().logger.isWarnEnabled();
    }

    @Override
    public void warn(String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLog(GemLogLevel.WARN, format, info, arguments);
    }

    @Override
    public void warn(Throwable t) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.WARN, "", t, info);
    }

    @Override
    public void warn(Throwable t, String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.WARN, format, t, info, arguments);
    }

    @Override
    public boolean isFatalEnabled() {
        LoggerInfo info = getTargetLoggerInfo();
        return info.logger.isFatalEnabled();
    }

    @Override
    public void fatal(String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLog(GemLogLevel.FATAL, format, info, arguments);
    }

    @Override
    public void fatal(Throwable t) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.FATAL, "", t, info);
    }

    @Override
    public void fatal(Throwable t, String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.FATAL, format, t, info, arguments);
    }

    @Override
    public boolean isErrorEnabled() {
        return getTargetLoggerInfo().logger.isErrorEnabled();
    }

    @Override
    public void error(String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();

        recordLog(GemLogLevel.ERROR, format, info, arguments);
    }

    @Override
    public void error(Throwable t) {
        LoggerInfo info = getTargetLoggerInfo();
        recordLogWithThrowable(GemLogLevel.ERROR, "", t, info);
    }

    @Override
    public void error(Throwable t, String format, Object... arguments) {
        LoggerInfo info = getTargetLoggerInfo();

        recordLogWithThrowable(GemLogLevel.ERROR, format, t, info, arguments);
    }
}
