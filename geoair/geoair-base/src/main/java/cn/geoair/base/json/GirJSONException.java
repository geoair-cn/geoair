package cn.geoair.base.json;

import cn.geoair.base.exception.GirException;
import cn.geoair.base.util.GutilStr;

public class GirJSONException extends GirException {

	/**
	 *
	 */
	private static final long serialVersionUID = -2014231003542321867L;

	public GirJSONException(Throwable e) {
		super(getMessage(e), e);
	}

	private static String getMessage(Throwable e) {
		if (null == e) {
			return GutilStr.NULL;
		}
		return GutilStr.format("{}: {}", e.getClass().getSimpleName(), e.getMessage());
	}

	public GirJSONException(String message) {
		super(message);
	}

	public GirJSONException(String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params));
	}

	public GirJSONException(String message, Throwable cause) {
		super(message, cause);
	}

	public GirJSONException(Throwable throwable, String messageTemplate, Object... params) {
		super(GutilStr.format(messageTemplate, params), throwable);
	}

}
