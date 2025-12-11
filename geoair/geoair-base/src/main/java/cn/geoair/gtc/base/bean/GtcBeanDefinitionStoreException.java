package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.util.GutilStr;


/**
 * Bean 定义异常
 * @author Ray
 *
 */
public class GtcBeanDefinitionStoreException extends GtcBeanException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1345140759210704903L;

	public GtcBeanDefinitionStoreException() {
		this("系统异常");
	}

	public GtcBeanDefinitionStoreException(String msg) {
		super(msg);
	}

	public GtcBeanDefinitionStoreException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GtcBeanDefinitionStoreException(Throwable e) {
		super(e);
	}

	public GtcBeanDefinitionStoreException(String messageTemplate, Object... params) {
		super(messageTemplate, params);
	}

	public GtcBeanDefinitionStoreException(Throwable throwable, String messageTemplate, Object... params) {
		this(GutilStr.format(messageTemplate, params),throwable);
	}

}
