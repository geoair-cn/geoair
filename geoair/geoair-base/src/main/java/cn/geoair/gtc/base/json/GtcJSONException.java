package cn.geoair.gtc.base.json;

import cn.geoair.gtc.base.exception.GtcException;
import cn.geoair.gtc.base.util.GutilStr;

public class GtcJSONException extends GtcException {

	/**
	 *
	 */
	private static final long serialVersionUID = -2014231003542321867L;



	public GtcJSONException(Throwable e) {
		super(getMessage(e), e);
	}

	private static String getMessage(Throwable e) {
		if (null == e) {
			return GutilStr.NULL;
		}
		return GutilStr.format("{}: {}", e.getClass().getSimpleName(), e.getMessage());
	}

	public GtcJSONException(String message) {
		super(message);
	}

	public GtcJSONException(String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params));
	}

	public GtcJSONException(String message, Throwable cause) {
		super(message, cause);
	}

	public GtcJSONException(Throwable throwable, String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params), throwable);
	}
}
