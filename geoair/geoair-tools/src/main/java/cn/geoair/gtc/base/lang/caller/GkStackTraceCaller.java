package cn.geoair.gtc.base.lang.caller;

import java.io.Serializable;

import cn.geoair.gtc.base.util.GutilStr;

/**
 * 通过StackTrace方式获取调用者。此方式效率最低，不推荐使用
 *
 * @author
 */
public class GkStackTraceCaller implements GkCaller, Serializable {
	private static final long serialVersionUID = 1L;
	private static final int OFFSET = 2;


	@Override
	public Class<?> getCaller() {
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		if (OFFSET + 1 >= stackTrace.length) {
			return null;
		}
		final String className = stackTrace[OFFSET + 1].getClassName();
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {

			throw new IllegalStateException(GutilStr.format( "[{}] not found!", className),e);
		}
	}

	@Override
	public String getCallerName() {
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		if (OFFSET + 1 >= stackTrace.length) {
			return null;
		}
		return stackTrace[OFFSET + 1].getClassName();
	}



	@Override
	public Class<?> getCallerCaller() {
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		if (OFFSET + 2 >= stackTrace.length) {
			return null;
		}
		final String className = stackTrace[OFFSET + 2].getClassName();
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(GutilStr.format("[{}] not found!", className),e);
		}
	}


	@Override
	public String getCallerCallerName() {
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		if (OFFSET + 2 >= stackTrace.length) {
			return null;
		}
		return stackTrace[OFFSET + 2].getClassName();
	}


	@Override
	public Class<?> getCaller(int depth) {
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		if (OFFSET + depth >= stackTrace.length) {
			return null;
		}
		final String className = stackTrace[OFFSET + depth].getClassName();
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(GutilStr.format("[{}] not found!", className),e);
		}
	}

	@Override
	public String getCallerName(int depth) {
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		if (OFFSET + depth >= stackTrace.length) {
			return null;
		}
		return stackTrace[OFFSET + depth].getClassName();
	}


	@Override
	public boolean isCalledBy(Class<?> clazz) {
		final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		for (final StackTraceElement element : stackTrace) {
			if (element.getClassName().equals(clazz.getName())) {
				return true;
			}
		}
		return false;
	}
}
