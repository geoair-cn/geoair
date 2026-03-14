package cn.geoair.base.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.geoair.base.tool.GkConsole;
import cn.geoair.base.util.GutilAssert;
import cn.geoair.base.util.GutilStr;

/**
 * 控制台打印日志的实现
 *
 * @author Ray
 *
 */

public class GirConsoleLog implements GiLogger {

	public static GirConsoleLog forName(String name) {
		return new GirConsoleLog(name);
	}

	private static final String logFormat = "[{date}] [{level}] {name}: {msg}";

	private final String name;

	private static GemLogLevel currentLevel = GemLogLevel.ALL;

	// -------------------------------------------------------------------------
	// Constructor

	/**
	 * 构造
	 * @param clazz 类
	 */
	public GirConsoleLog(Class<?> clazz) {
		this.name = (null == clazz) ? GutilStr.NULL : clazz.getName();
	}

	/**
	 * 构造
	 * @param name 类名
	 */
	public GirConsoleLog(String name) {
		this.name = name;
	}

	/// @Override
	public String getName() {
		return this.name;
	}

	/**
	 * 设置自定义的日志显示级别
	 * @param customLevel 自定义级别
	 */
	public static void setLevel(GemLogLevel customLevel) {
		GutilAssert.notNull(customLevel, () -> "级别不能为null");
		currentLevel = customLevel;
	}

	// -------------------------------------------------------------------------
	public void log(GemLogLevel level, Throwable t, String format, Object... arguments) {
		if (false == isEnabled(level)) {
			return;
		}
		Map<String, Object> map = new HashMap<>();
		map.put("date", new Date());
		map.put("level", level.toString());
		map.put("name", this.name);
		map.put("msg", GutilStr.format(format, arguments));
		final String logMsg = GutilStr.format(logFormat, map);
		// WARN以上级别打印至System.err
		if (level.ordinal() >= GemLogLevel.WARN.ordinal()) {
			GkConsole.error(t, logMsg);
		}
		else {
			GkConsole.log(t, logMsg);
		}
	}

	public boolean isEnabled(GemLogLevel level) {
		return currentLevel.compareTo(level) <= 0;
	}

	// -------------------------------------------------------------------------
	// Fatal
	@Override
	public boolean isFatalEnabled() {
		return isEnabled(GemLogLevel.FATAL);
	}

	@Override
	public void fatal(String format, Object... arguments) {
		log(GemLogLevel.FATAL, null, format, arguments);
	}

	@Override
	public void fatal(Throwable t) {
		log(GemLogLevel.FATAL, t, null);

	}

	@Override
	public void fatal(Throwable t, String format, Object... arguments) {
		log(GemLogLevel.FATAL, t, format, arguments);

	}

	// -------------------------------------------------------------------------
	// Error
	@Override
	public boolean isErrorEnabled() {
		return isEnabled(GemLogLevel.ERROR);
	}

	@Override
	public void error(Throwable t, String format, Object... arguments) {
		log(GemLogLevel.ERROR, t, format, arguments);
	}

	@Override
	public void error(String format, Object... arguments) {
		log(GemLogLevel.ERROR, null, format, arguments);

	}

	@Override
	public void error(Throwable t) {
		log(GemLogLevel.ERROR, t, null);
	}

	// -------------------------------------------------------------------------
	// Warn
	@Override
	public boolean isWarnEnabled() {
		return isEnabled(GemLogLevel.WARN);
	}

	@Override
	public void warn(Throwable t, String format, Object... arguments) {
		log(GemLogLevel.WARN, t, format, arguments);
	}

	@Override
	public void warn(String format, Object... arguments) {
		log(GemLogLevel.WARN, null, format, arguments);

	}

	@Override
	public void warn(Throwable t) {
		log(GemLogLevel.WARN, t, null);

	}

	// -------------------------------------------------------------------------
	// Info
	@Override
	public boolean isInfoEnabled() {
		return isEnabled(GemLogLevel.INFO);
	}

	@Override
	public void info(Throwable t, String format, Object... arguments) {
		log(GemLogLevel.INFO, t, format, arguments);
	}

	@Override
	public void info(String format, Object... arguments) {
		log(GemLogLevel.INFO, null, format, arguments);
	}

	@Override
	public void info(Throwable t) {
		log(GemLogLevel.INFO, t, null);
	}

	// -------------------------------------------------------------------------
	// Debug
	@Override
	public boolean isDebugEnabled() {
		return isEnabled(GemLogLevel.DEBUG);
	}

	@Override
	public void debug(Throwable t, String format, Object... arguments) {
		log(GemLogLevel.DEBUG, t, format, arguments);
	}

	@Override
	public void debug(String format, Object... arguments) {
		log(GemLogLevel.DEBUG, null, format, arguments);
	}

	@Override
	public void debug(Throwable t) {
		log(GemLogLevel.DEBUG, t, null);

	}

	// -------------------------------------------------------------------------
	@Override
	public boolean isTraceEnabled() {
		return isEnabled(GemLogLevel.TRACE);
	}

	@Override
	public void trace(Throwable t, String format, Object... arguments) {
		log(GemLogLevel.TRACE, t, format, arguments);
	}

	@Override
	public void trace(String format, Object... arguments) {
		log(GemLogLevel.TRACE, null, format, arguments);
	}

	@Override
	public void trace(Throwable t) {
		log(GemLogLevel.TRACE, t, null);
	}

}
