package cn.geoair.sdk;

import cn.geoair.base.exception.GirException;

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
