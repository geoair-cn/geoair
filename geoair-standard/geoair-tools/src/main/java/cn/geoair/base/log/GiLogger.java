package cn.geoair.base.log;

/**
 * 通用日志接口
 *
 * @author Ray
 * @since 1.0
 */
public interface GiLogger {

    /**
     * 检查致命错误（FATAL）级别日志是否启用
     *
     * <p>建议在构造复杂的日志消息前调用此方法进行判断，以避免不必要的性能开销
     *
     * @return {@code true} 如果 FATAL 级别日志已启用，否则返回 {@code false}
     */
    boolean isFatalEnabled();

    /**
     * 记录致命错误（FATAL）级别的日志
     *
     * <p>FATAL 级别表示严重错误，通常导致应用程序终止运行
     *
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void fatal(String format, Object... arguments);

    /**
     * 记录致命错误（FATAL）级别的异常日志
     *
     * <p>仅记录异常堆栈信息，不附带额外的自定义消息
     *
     * @param t 需要记录的异常对象
     */
    void fatal(Throwable t);

    /**
     * 记录带自定义消息的致命错误（FATAL）级别异常日志
     *
     * @param t 需要记录的异常对象
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void fatal(Throwable t, String format, Object... arguments);

    /**
     * 检查错误（ERROR）级别日志是否启用
     *
     * <p>建议在构造复杂的日志消息前调用此方法进行判断，以避免不必要的性能开销
     *
     * @return {@code true} 如果 ERROR 级别日志已启用，否则返回 {@code false}
     */
    boolean isErrorEnabled();

    /**
     * 记录错误（ERROR）级别的日志
     *
     * <p>ERROR 级别表示错误事件，可能不影响应用程序继续运行但需要被关注
     *
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void error(String format, Object... arguments);

    /**
     * 记录错误（ERROR）级别的异常日志
     *
     * <p>仅记录异常堆栈信息，不附带额外的自定义消息
     *
     * @param t 需要记录的异常对象
     */
    void error(Throwable t);

    /**
     * 记录带自定义消息的错误（ERROR）级别异常日志
     *
     * @param t 需要记录的异常对象
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void error(Throwable t, String format, Object... arguments);

    /**
     * 检查警告（WARN）级别日志是否启用
     *
     * <p>建议在构造复杂的日志消息前调用此方法进行判断，以避免不必要的性能开销
     *
     * @return {@code true} 如果 WARN 级别日志已启用，否则返回 {@code false}
     */
    boolean isWarnEnabled();

    /**
     * 记录警告（WARN）级别的日志
     *
     * <p>WARN 级别表示潜在的错误情况或非预期的事件，但应用程序仍可正常运行
     *
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void warn(String format, Object... arguments);

    /**
     * 记录警告（WARN）级别的异常日志
     *
     * <p>仅记录异常堆栈信息，不附带额外的自定义消息
     *
     * @param t 需要记录的异常对象
     */
    void warn(Throwable t);

    /**
     * 记录带自定义消息的警告（WARN）级别异常日志
     *
     * @param t 需要记录的异常对象
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void warn(Throwable t, String format, Object... arguments);

    /**
     * 检查信息（INFO）级别日志是否启用
     *
     * <p>建议在构造复杂的日志消息前调用此方法进行判断，以避免不必要的性能开销
     *
     * @return {@code true} 如果 INFO 级别日志已启用，否则返回 {@code false}
     */
    boolean isInfoEnabled();

    /**
     * 记录信息（INFO）级别的日志
     *
     * <p>INFO 级别表示应用程序运行过程中的重要事件，用于了解系统的整体运行状态
     *
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void info(String format, Object... arguments);

    /**
     * 记录信息（INFO）级别的异常日志
     *
     * <p>仅记录异常堆栈信息，不附带额外的自定义消息
     *
     * @param t 需要记录的异常对象
     */
    void info(Throwable t);

    /**
     * 记录带自定义消息的信息（INFO）级别异常日志
     *
     * @param t 需要记录的异常对象
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void info(Throwable t, String format, Object... arguments);

    /**
     * 检查调试（DEBUG）级别日志是否启用
     *
     * <p>建议在构造复杂的日志消息前调用此方法进行判断，以避免不必要的性能开销
     *
     * @return {@code true} 如果 DEBUG 级别日志已启用，否则返回 {@code false}
     */
    boolean isDebugEnabled();

    /**
     * 记录调试（DEBUG）级别的日志
     *
     * <p>DEBUG 级别用于记录对调试应用程序有帮助的详细信息，通常在开发环境中启用
     *
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void debug(String format, Object... arguments);

    /**
     * 记录调试（DEBUG）级别的异常日志
     *
     * <p>仅记录异常堆栈信息，不附带额外的自定义消息
     *
     * @param t 需要记录的异常对象
     */
    void debug(Throwable t);

    /**
     * 记录带自定义消息的调试（DEBUG）级别异常日志
     *
     * @param t 需要记录的异常对象
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void debug(Throwable t, String format, Object... arguments);

    /**
     * 检查追踪（TRACE）级别日志是否启用
     *
     * <p>建议在构造复杂的日志消息前调用此方法进行判断，以避免不必要的性能开销
     *
     * @return {@code true} 如果 TRACE 级别日志已启用，否则返回 {@code false}
     */
    boolean isTraceEnabled();

    /**
     * 记录追踪（TRACE）级别的日志
     *
     * <p>TRACE 是最详细的日志级别，用于记录程序的细粒度执行流程，通常在问题排查时使用
     *
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void trace(String format, Object... arguments);

    /**
     * 记录追踪（TRACE）级别的异常日志
     *
     * <p>仅记录异常堆栈信息，不附带额外的自定义消息
     *
     * @param t 需要记录的异常对象
     */
    void trace(Throwable t);

    /**
     * 记录带自定义消息的追踪（TRACE）级别异常日志
     *
     * @param t 需要记录的异常对象
     * @param format 日志消息格式字符串，支持 {@code {}} 作为占位符
     * @param arguments 格式化参数，用于替换消息中的占位符
     */
    void trace(Throwable t, String format, Object... arguments);
}
