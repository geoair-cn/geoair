package cn.geoair.map.dynamic.geoserver.exception;

import cn.geoair.base.exception.GirException;

/** 服务发布异常 */
public class ServicePublishException extends GirException {

	// 错误码
	private String errorCode;

	public ServicePublishException(String message) {
		super(message);
	}

	public ServicePublishException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServicePublishException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	// getter/setter
	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

}
