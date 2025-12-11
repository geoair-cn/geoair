package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.util.GutilStr;


/**
 * 不是唯一的Bean异常
 * @author Ray
 *
 */
public class GtcNoUniqueBeanException extends GtcNoSuchBeanException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1345140759210704903L;

	public GtcNoUniqueBeanException() {
		this("系统异常");
	}

	public GtcNoUniqueBeanException(String msg) {
		super(msg);
	}

	public GtcNoUniqueBeanException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GtcNoUniqueBeanException(Throwable e) {
		super(e);
	}

	public GtcNoUniqueBeanException(String messageTemplate, Object... params) {
		super(messageTemplate, params);
	}

	public GtcNoUniqueBeanException(Throwable throwable, String messageTemplate, Object... params) {
		this(GutilStr.format(messageTemplate, params),throwable);
	}

}
