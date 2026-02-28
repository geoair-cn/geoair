package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.exception.GirException;
import cn.geoair.gtc.base.util.GutilStr;

/**
 *
 *
 */
public class GirBeanException extends GirException {

	/**
	 *
	 */
	private static final long serialVersionUID = 2171952030749988728L;

	/**
	 *
	 */

	public GirBeanException() {
		this("系统异常");
	}

	public GirBeanException(String msg) {
		super(msg);
	}

	public GirBeanException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GirBeanException(Throwable e) {
		super(e);
	}

	public GirBeanException(String messageTemplate, Object... params) {
		super(messageTemplate, params);
	}

	public GirBeanException(Throwable throwable, String messageTemplate, Object... params) {
		this(GutilStr.format(messageTemplate, params), throwable);
	}

	/*
	 * public gtcBeanException(Class<?> clazz,String msg) { this(clazz,msg, null); }
	 *
	 * public gtcBeanException(Class<?> clazz,String msg, Throwable cause) { super(msg,
	 * cause); }
	 *
	 * public gtcBeanException(Constructor<?> ctor, String msg, Throwable ex) { super(msg,
	 * ex); }
	 */

}
