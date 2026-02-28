package cn.geoair.gtc.base.exception;

import cn.geoair.gtc.base.util.GutilStr;

/**
 * @author ：张俊
 * @date ：Created in 2024/11/27 16:52 @description： 业务异常
 */
public class GirEBizException extends GirException {

	public GirEBizException() {
		this("业务异常");
	}

	public GirEBizException(String msg) {
		super(msg);
	}

	public GirEBizException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GirEBizException(Throwable e) {
		super(e);
	}

	public GirEBizException(String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params));
	}

	public GirEBizException(Throwable throwable, String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params), throwable);
	}

}
