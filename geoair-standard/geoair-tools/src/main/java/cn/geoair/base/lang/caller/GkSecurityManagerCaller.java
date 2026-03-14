package cn.geoair.base.lang.caller;

import java.io.Serializable;

/**
 * {@link SecurityManager} 方式获取调用者
 *
 * @author
 */
public class GkSecurityManagerCaller extends SecurityManager implements GkCaller, Serializable {

	private static final long serialVersionUID = 1L;

	private static final int OFFSET = 1;

	@Override
	public Class<?> getCaller() {
		final Class<?>[] context = getClassContext();
		if (null != context && (OFFSET + 1) < context.length) {
			return context[OFFSET + 1];
		}
		return null;
	}

	@Override
	public String getCallerName() {
		final Class<?>[] context = getClassContext();
		if (null != context && (OFFSET + 1) < context.length) {
			return context[OFFSET + 1].getName();
		}
		return null;
	}

	@Override
	public Class<?> getCallerCaller() {
		final Class<?>[] context = getClassContext();
		if (null != context && (OFFSET + 2) < context.length) {
			return context[OFFSET + 2];
		}
		return null;
	}

	@Override
	public String getCallerCallerName() {
		final Class<?>[] context = getClassContext();
		if (null != context && (OFFSET + 2) < context.length) {
			return context[OFFSET + 2].getName();
		}
		return null;
	}

	@Override
	public Class<?> getCaller(int depth) {
		final Class<?>[] context = getClassContext();
		if (null != context && (OFFSET + depth) < context.length) {
			return context[OFFSET + depth];
		}
		return null;
	}

	@Override
	public String getCallerName(int depth) {
		final Class<?>[] context = getClassContext();
		if (null != context && (OFFSET + depth) < context.length) {
			return context[OFFSET + depth].getName();
		}
		return null;
	}

	@Override
	public boolean isCalledBy(Class<?> clazz) {
		final Class<?>[] classes = getClassContext();
		if ((classes != null && classes.length != 0)) {
			for (Class<?> contextClass : classes) {
				if (contextClass.equals(clazz)) {
					return true;
				}
			}
		}
		return false;
	}

}
