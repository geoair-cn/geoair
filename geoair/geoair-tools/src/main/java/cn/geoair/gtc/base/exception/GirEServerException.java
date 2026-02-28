package cn.geoair.gtc.base.exception;

import cn.geoair.gtc.base.util.GutilStr;

/**
 * @author ：张俊
 * @date ：Created in 2024/11/27 16:52 @description： 服务器异常
 */
public class GirEServerException extends GirException {

	public GirEServerException() {
		this("服务异常");
	}

	public GirEServerException(String msg) {
		super(msg);
	}

	public GirEServerException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GirEServerException(Throwable e) {
		super(e);
	}

	public GirEServerException(String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params));
	}

	public GirEServerException(Throwable throwable, String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params), throwable);
	}

}
