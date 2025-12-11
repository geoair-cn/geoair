package cn.geoair.gtc.sdk;

import cn.geoair.gtc.base.exception.GtcException;

@SuppressWarnings("serial")
public class GtcSdkException extends GtcException {

	public GtcSdkException() {
		this("SDK异常");
	}

	public GtcSdkException(String msg) {
		super(msg);
	}

	public GtcSdkException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
