package cn.geoair.spi.log;

import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.base.log.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log4Gir {

    public enum LogType {
        APPACHECOMMONS,
        HUTOOL,
        SLF4J,
        CONSOLE
    }

    private static LogType logType;

    static {
        GkMethodHand.implFromClass(GirLogger.class);
        GkMethodHand.implFromClass(GirLoggerFactory.class);
        GirLoggerFactory.setLoggerProvider(Log4Gir::getLogger);
        Log4Gir.setLogType(LogProviderResolver.resolve());
    }

    public static void setLogType(LogType logType2) {
        logType = logType2;
    }

    @GaMethodHandImpl(
            implClass = GirLogger.class,
            implMethod = "getLoger",
            type = ImplType.expectfirst)
    public static GiLogger getLoger(String name) {
        return getLogger(name);
    }

    @GaMethodHandImpl(
            implClass = GirLoggerFactory.class,
            implMethod = "getLogger",
            type = ImplType.expectfirst)
    public static GiLogger getLogger(String name) {
        switch (logType) {
            case SLF4J:
                return Slf4jLog.createLog(name);
            case APPACHECOMMONS:
                return ApacheCommonsLog.createLog(name);
            case HUTOOL:
                return HutoolLog.createLog(name);
            default:
                return GirConsoleLog.forName(name);
        }
    }

    private static class Slf4jLog implements GiLogger {

        private final Logger logger;

        private Slf4jLog(String name) {
            this.logger = LoggerFactory.getLogger(name);
        }

        public static GiLogger createLog(String name) {
            return new Slf4jLog(name);
        }

        @Override
        public boolean isFatalEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public void fatal(String format, Object... arguments) {
            if (!isFatalEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.error(tp.getMessage(), tp.getThrowable());
            } else {
                logger.error(tp.getMessage());
            }
        }

        @Override
        public void fatal(Throwable t) {
            if (!isFatalEnabled()) {
                return;
            }
            logger.error("", t);
        }

        @Override
        public void fatal(Throwable t, String format, Object... arguments) {
            if (!isFatalEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.error(tp.getMessage(), t);
        }

        @Override
        public void error(String format, Object... arguments) {
            if (!isErrorEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.error(tp.getMessage(), tp.getThrowable());
            } else {
                logger.error(tp.getMessage());
            }
        }

        @Override
        public void error(Throwable t) {
            if (!isErrorEnabled()) {
                return;
            }
            logger.error("", t);
        }

        @Override
        public void error(Throwable t, String format, Object... arguments) {
            if (!isErrorEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.error(tp.getMessage(), t);
        }

        @Override
        public void warn(String format, Object... arguments) {
            if (!isWarnEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.warn(tp.getMessage(), tp.getThrowable());
            } else {
                logger.warn(tp.getMessage());
            }
        }

        @Override
        public void warn(Throwable t) {
            if (!isWarnEnabled()) {
                return;
            }
            logger.warn("", t);
        }

        @Override
        public void warn(Throwable t, String format, Object... arguments) {
            if (!isWarnEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.warn(tp.getMessage(), t);
        }

        @Override
        public void info(String format, Object... arguments) {
            if (!isInfoEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.info(tp.getMessage(), tp.getThrowable());
            } else {
                logger.info(tp.getMessage());
            }
        }

        @Override
        public void info(Throwable t) {
            if (!isInfoEnabled()) {
                return;
            }
            logger.info("", t);
        }

        @Override
        public void info(Throwable t, String format, Object... arguments) {
            if (!isInfoEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.info(tp.getMessage(), t);
        }

        @Override
        public void debug(String format, Object... arguments) {
            if (!isDebugEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.debug(tp.getMessage(), tp.getThrowable());
            } else {
                logger.debug(tp.getMessage());
            }
        }

        @Override
        public void debug(Throwable t) {
            if (!isDebugEnabled()) {
                return;
            }
            logger.debug("", t);
        }

        @Override
        public void debug(Throwable t, String format, Object... arguments) {
            if (!isDebugEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.debug(tp.getMessage(), t);
        }

        @Override
        public void trace(String format, Object... arguments) {
            if (!isTraceEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.trace(tp.getMessage(), tp.getThrowable());
            } else {
                logger.trace(tp.getMessage());
            }
        }

        @Override
        public void trace(Throwable t) {
            if (!isTraceEnabled()) {
                return;
            }
            logger.trace("", t);
        }

        @Override
        public void trace(Throwable t, String format, Object... arguments) {
            if (!isTraceEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.trace(tp.getMessage(), t);
        }
    }

    private static class ApacheCommonsLog implements GiLogger {

        private final org.apache.commons.logging.Log logger;

        private ApacheCommonsLog(String name) {
            this.logger = org.apache.commons.logging.LogFactory.getLog(name);
        }

        public static GiLogger createLog(String name) {
            return new ApacheCommonsLog(name);
        }

        @Override
        public boolean isFatalEnabled() {
            return logger.isFatalEnabled();
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public void fatal(String format, Object... arguments) {
            if (!isFatalEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.fatal(tp.getMessage(), tp.getThrowable());
            } else {
                logger.fatal(tp.getMessage());
            }
        }

        @Override
        public void fatal(Throwable t) {
            if (!isFatalEnabled()) {
                return;
            }
            logger.fatal(t);
        }

        @Override
        public void fatal(Throwable t, String format, Object... arguments) {
            if (!isFatalEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.fatal(tp.getMessage(), t);
        }

        @Override
        public void error(String format, Object... arguments) {
            if (!isErrorEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.error(tp.getMessage(), tp.getThrowable());
            } else {
                logger.error(tp.getMessage());
            }
        }

        @Override
        public void error(Throwable t) {
            if (!isErrorEnabled()) {
                return;
            }
            logger.error(t);
        }

        @Override
        public void error(Throwable t, String format, Object... arguments) {
            if (!isErrorEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.error(tp.getMessage(), t);
        }

        @Override
        public void warn(String format, Object... arguments) {
            if (!isWarnEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.warn(tp.getMessage(), tp.getThrowable());
            } else {
                logger.warn(tp.getMessage());
            }
        }

        @Override
        public void warn(Throwable t) {
            if (!isWarnEnabled()) {
                return;
            }
            logger.warn(t);
        }

        @Override
        public void warn(Throwable t, String format, Object... arguments) {
            if (!isWarnEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.warn(tp.getMessage(), t);
        }

        @Override
        public void info(String format, Object... arguments) {
            if (!isInfoEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.info(tp.getMessage(), tp.getThrowable());
            } else {
                logger.info(tp.getMessage());
            }
        }

        @Override
        public void info(Throwable t) {
            if (!isInfoEnabled()) {
                return;
            }
            logger.info(t);
        }

        @Override
        public void info(Throwable t, String format, Object... arguments) {
            if (!isInfoEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.info(tp.getMessage(), t);
        }

        @Override
        public void debug(String format, Object... arguments) {
            if (!isDebugEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.debug(tp.getMessage(), tp.getThrowable());
            } else {
                logger.debug(tp.getMessage());
            }
        }

        @Override
        public void debug(Throwable t) {
            if (!isDebugEnabled()) {
                return;
            }
            logger.debug(t);
        }

        @Override
        public void debug(Throwable t, String format, Object... arguments) {
            if (!isDebugEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.debug(tp.getMessage(), t);
        }

        @Override
        public void trace(String format, Object... arguments) {
            if (!isTraceEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.trace(tp.getMessage(), tp.getThrowable());
            } else {
                logger.trace(tp.getMessage());
            }
        }

        @Override
        public void trace(Throwable t) {
            if (!isTraceEnabled()) {
                return;
            }
            logger.trace(t);
        }

        @Override
        public void trace(Throwable t, String format, Object... arguments) {
            if (!isTraceEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.trace(tp.getMessage(), t);
        }
    }

    private static class HutoolLog implements GiLogger {

        private final cn.hutool.log.Log logger;

        private HutoolLog(String name) {
            this.logger = cn.hutool.log.LogFactory.get(name);
        }

        public static GiLogger createLog(String name) {
            return new HutoolLog(name);
        }

        @Override
        public boolean isFatalEnabled() {
            return logger.isEnabled(cn.hutool.log.level.Level.FATAL);
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public void fatal(String format, Object... arguments) {
            if (!isFatalEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.error(tp.getThrowable(), tp.getMessage());
            } else {
                logger.error(tp.getMessage());
            }
        }

        @Override
        public void fatal(Throwable t) {
            if (!isFatalEnabled()) {
                return;
            }
            logger.error(t);
        }

        @Override
        public void fatal(Throwable t, String format, Object... arguments) {
            if (!isFatalEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.error(t, tp.getMessage());
        }

        @Override
        public void error(String format, Object... arguments) {
            if (!isErrorEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.error(tp.getThrowable(), tp.getMessage());
            } else {
                logger.error(tp.getMessage());
            }
        }

        @Override
        public void error(Throwable t) {
            if (!isErrorEnabled()) {
                return;
            }
            logger.error(t);
        }

        @Override
        public void error(Throwable t, String format, Object... arguments) {
            if (!isErrorEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.error(t, tp.getMessage());
        }

        @Override
        public void warn(String format, Object... arguments) {
            if (!isWarnEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.warn(tp.getThrowable(), tp.getMessage());
            } else {
                logger.warn(tp.getMessage());
            }
        }

        @Override
        public void warn(Throwable t) {
            if (!isWarnEnabled()) {
                return;
            }
            logger.warn(t);
        }

        @Override
        public void warn(Throwable t, String format, Object... arguments) {
            if (!isWarnEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.warn(t, tp.getMessage());
        }

        @Override
        public void info(String format, Object... arguments) {
            if (!isInfoEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.info(tp.getThrowable(), tp.getMessage());
            } else {
                logger.info(tp.getMessage());
            }
        }

        @Override
        public void info(Throwable t) {
            if (!isInfoEnabled()) {
                return;
            }
            logger.info(t);
        }

        @Override
        public void info(Throwable t, String format, Object... arguments) {
            if (!isInfoEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.info(t, tp.getMessage());
        }

        @Override
        public void debug(String format, Object... arguments) {
            if (!isDebugEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.debug(tp.getThrowable(), tp.getMessage());
            } else {
                logger.debug(tp.getMessage());
            }
        }

        @Override
        public void debug(Throwable t) {
            if (!isDebugEnabled()) {
                return;
            }
            logger.debug(t);
        }

        @Override
        public void debug(Throwable t, String format, Object... arguments) {
            if (!isDebugEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.debug(t, tp.getMessage());
        }

        @Override
        public void trace(String format, Object... arguments) {
            if (!isTraceEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            if (tp.getThrowable() != null) {
                logger.trace(tp.getThrowable(), tp.getMessage());
            } else {
                logger.trace(tp.getMessage());
            }
        }

        @Override
        public void trace(Throwable t) {
            if (!isTraceEnabled()) {
                return;
            }
            logger.trace(t);
        }

        @Override
        public void trace(Throwable t, String format, Object... arguments) {
            if (!isTraceEnabled()) {
                return;
            }
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            logger.trace(t, tp.getMessage());
        }
    }
}
