package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.util.GutilStr;


/**
 * 不是唯一的Bean异常
 * @author Ray
 *
 */
public class GirNoUniqueBeanException extends GirNoSuchBeanException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1345140759210704903L;

	public GirNoUniqueBeanException() {
		this("系统异常");
	}

	public GirNoUniqueBeanException(String msg) {
		super(msg);
	}

	public GirNoUniqueBeanException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GirNoUniqueBeanException(Throwable e) {
		super(e);
	}

	public GirNoUniqueBeanException(String messageTemplate, Object... params) {
		super(messageTemplate, params);
	}

	public GirNoUniqueBeanException(Throwable throwable, String messageTemplate, Object... params) {
		this(GutilStr.format(messageTemplate, params),throwable);
	}

}
