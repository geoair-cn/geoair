package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.util.GutilStr;

/**
 * 获取不到Bean的异常
 * @author Ray
 *
 */
public class GtcNoSuchBeanException extends GtcBeanException {

	/**
	 *
	 */
	private static final long serialVersionUID = -341043413233724962L;


	public GtcNoSuchBeanException() {
		this("系统异常");
	}

	public GtcNoSuchBeanException(String msg) {
		super(msg);
	}

	public GtcNoSuchBeanException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GtcNoSuchBeanException(Throwable e) {
		super(e);
	}

	public GtcNoSuchBeanException(String messageTemplate, Object... params) {
		super(messageTemplate, params);
	}

	public GtcNoSuchBeanException(Throwable throwable, String messageTemplate, Object... params) {
		this(GutilStr.format(messageTemplate, params),throwable);
	}

}
