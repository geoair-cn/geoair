package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.exception.GtcException;
import cn.geoair.gtc.base.util.GutilStr;


/**
 *
 *
 */
public class GtcBeanException extends GtcException {


	/**
	 *
	 */
	private static final long serialVersionUID = 2171952030749988728L;

	/**
	 *
	 */

	public GtcBeanException() {
		this("系统异常");
	}

	public GtcBeanException(String msg) {
		super(msg);
	}

	public GtcBeanException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GtcBeanException(Throwable e) {
		super(e);
	}

	public GtcBeanException(String messageTemplate, Object... params) {
		super(messageTemplate, params);
	}

	public GtcBeanException(Throwable throwable, String messageTemplate, Object... params) {
		this(GutilStr.format(messageTemplate, params),throwable);
	}

	/*
	public  gtcBeanException(Class<?> clazz,String msg) {
		this(clazz,msg, null);
	}

	public  gtcBeanException(Class<?> clazz,String msg, Throwable cause) {
		super(msg, cause);
	}

	public  gtcBeanException(Constructor<?> ctor, String msg, Throwable ex) {
		super(msg, ex);
	}
	*/
}
