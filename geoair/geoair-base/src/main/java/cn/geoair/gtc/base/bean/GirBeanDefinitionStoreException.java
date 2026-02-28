package cn.geoair.gtc.base.bean;

import cn.geoair.gtc.base.util.GutilStr;

/**
 * Bean 定义异常
 *
 * @author Ray
 *
 */
public class GirBeanDefinitionStoreException extends GirBeanException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1345140759210704903L;

	public GirBeanDefinitionStoreException() {
		this("系统异常");
	}

	public GirBeanDefinitionStoreException(String msg) {
		super(msg);
	}

	public GirBeanDefinitionStoreException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GirBeanDefinitionStoreException(Throwable e) {
		super(e);
	}

	public GirBeanDefinitionStoreException(String messageTemplate, Object... params) {
		super(messageTemplate, params);
	}

	public GirBeanDefinitionStoreException(Throwable throwable, String messageTemplate, Object... params) {
		this(GutilStr.format(messageTemplate, params), throwable);
	}

}
