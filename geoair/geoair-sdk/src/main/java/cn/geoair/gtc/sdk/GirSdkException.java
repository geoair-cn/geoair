package cn.geoair.gtc.sdk;

import cn.geoair.gtc.base.exception.GirException;

@SuppressWarnings("serial")
public class GirSdkException extends GirException {

	public GirSdkException() {
		this("SDK异常");
	}

	public GirSdkException(String msg) {
		super(msg);
	}

	public GirSdkException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
