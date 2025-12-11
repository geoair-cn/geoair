package cn.geoair.gtc.base.exception;


import cn.geoair.gtc.base.util.GutilStr;

/**
 *
 *
 */
public class GtcException extends RuntimeException{


	/**
	 *
	 */
	private static final long serialVersionUID = 1473955058515130479L;



	public GtcException() {
		this("系统异常");
	}

	public GtcException(String msg) {
		super(msg);
	}

	public GtcException(String msg, Throwable cause) {
		super(msg, cause);
	}


	public GtcException(Throwable e) {
		super(e);
	}

	public GtcException(String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params));
	}

	public GtcException(Throwable throwable, String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params), throwable);
	}

}
