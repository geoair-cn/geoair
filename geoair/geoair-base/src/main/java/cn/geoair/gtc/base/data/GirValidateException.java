package cn.geoair.gtc.base.data;

import cn.geoair.gtc.base.exception.GirException;
import cn.geoair.gtc.base.util.GutilStr;

/**
 * 异常
 *
 */

@SuppressWarnings("serial")
public class GirValidateException extends GirException {

	public GirValidateException() {
		this("验证异常");
	}
	public GirValidateException(String msg) {
		super(msg);
	}

	public GirValidateException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GirValidateException(String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params));
	}

	public GirValidateException(Throwable throwable, String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params), throwable);
	}

}
