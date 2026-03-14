package cn.geoair.map.dynamic.geoserver.enums;

/** OGC 服务类型枚举 */
public enum OgcServiceType {

	WMS("WMS", "地图服务"), WFS("WFS", "要素服务"), WCS("WCS", "覆盖服务");

	private final String code;

	private final String desc;

	OgcServiceType(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public String getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}

}
