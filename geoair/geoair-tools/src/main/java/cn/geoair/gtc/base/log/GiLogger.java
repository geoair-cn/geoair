package cn.geoair.gtc.base.log;

/**
 * 通用日志接口
 *
 * @author Ray
 *
 */
public interface GiLogger {

	/**
	 * Is fatal logging currently enabled?
	 */
	boolean isFatalEnabled();

	/**
	 * fatal
	 */
	void fatal(String format, Object... arguments);

	void fatal(Throwable t);

	void fatal(Throwable t, String format, Object... arguments);

	/**
	 * Is error logging currently enabled?
	 */
	boolean isErrorEnabled();

	/**
	 * error
	 */
	void error(String format, Object... arguments);

	void error(Throwable t);

	void error(Throwable t, String format, Object... arguments);

	/**
	 * Is warn logging currently enabled?
	 */
	boolean isWarnEnabled();

	/**
	 * warn
	 */
	void warn(String format, Object... arguments);

	void warn(Throwable t);

	void warn(Throwable t, String format, Object... arguments);

	/**
	 * Is info logging currently enabled?
	 */
	boolean isInfoEnabled();

	/**
	 * info
	 */
	void info(String format, Object... arguments);

	void info(Throwable t);

	void info(Throwable t, String format, Object... arguments);

	/**
	 * Is debug logging currently enabled?
	 */
	boolean isDebugEnabled();

	/**
	 * debug
	 */
	void debug(String format, Object... arguments);

	void debug(Throwable t);

	void debug(Throwable t, String format, Object... arguments);

	/**
	 * Is trace logging currently enabled?
	 */
	boolean isTraceEnabled();

	/**
	 * trace
	 */
	void trace(String format, Object... arguments);

	void trace(Throwable t);

	void trace(Throwable t, String format, Object... arguments);

}
