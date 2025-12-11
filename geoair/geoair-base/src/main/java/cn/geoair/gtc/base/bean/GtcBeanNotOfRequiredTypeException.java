package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.util.GutilStr;


/**
 * Bean类型不匹配的异常
 * @author Ray
 *
 */
public class GtcBeanNotOfRequiredTypeException extends GtcBeanException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1345140759210704903L;

	public GtcBeanNotOfRequiredTypeException() {
		this("系统异常");
	}

	public GtcBeanNotOfRequiredTypeException(String msg) {
		super(msg);
	}

	public GtcBeanNotOfRequiredTypeException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GtcBeanNotOfRequiredTypeException(Throwable e) {
		super(e);
	}

	public GtcBeanNotOfRequiredTypeException(String messageTemplate, Object... params) {
		super(messageTemplate, params);
	}

	public GtcBeanNotOfRequiredTypeException(Throwable throwable, String messageTemplate, Object... params) {
		this(GutilStr.format(messageTemplate, params),throwable);
	}

}
