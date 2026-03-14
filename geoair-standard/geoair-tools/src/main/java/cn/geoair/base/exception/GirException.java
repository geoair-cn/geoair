package cn.geoair.base.exception;

import cn.geoair.base.util.GutilStr;

/**
 *
 *
 */
public class GirException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1473955058515130479L;

	public GirException() {
		this("系统异常");
	}

	public GirException(String msg) {
		super(msg);
	}

	public GirException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GirException(Throwable e) {
		super(e);
	}

	public GirException(String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params));
	}

	public GirException(Throwable throwable, String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params), throwable);
	}

}
