package cn.geoair.gtc.base.data;

import cn.geoair.gtc.base.exception.GtcException;
import cn.geoair.gtc.base.util.GutilStr;

/**
 * 异常
 *
 */

@SuppressWarnings("serial")
public class GtcValidateException extends GtcException {

	public GtcValidateException() {
		this("验证异常");
	}
	public GtcValidateException(String msg) {
		super(msg);
	}

	public GtcValidateException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GtcValidateException(String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params));
	}

	public GtcValidateException(Throwable throwable, String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params), throwable);
	}

}
