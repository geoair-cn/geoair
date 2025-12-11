package cn.geoair.gtc.base.data.common;
import cn.geoair.gtc.base.data.GiVisualValuable;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

public enum GemStatus implements GiVisualValuable<Integer> {

	启用(1),
	禁用(0);

	@GaModelField(isID = true)
	private Integer code;

	GemStatus(Integer code) {
		this.code = code;
	}

	public Integer getCode() {
		return this.code;
	}

	@Override
	public String display() {
		return this.name();
	}

	@Override
	public Integer value() {
		return this.code;
	}

	public static GemStatus findByValue(Integer value, GemStatus defaultE) {
		for (GemStatus pe : GemStatus.values()) {
			if (pe.code.equals(value)) {
				return pe;
			}
		}
		return defaultE;
	}

	public static GemStatus findByValue(int value, GemStatus defaultE) {
		for (GemStatus pe : GemStatus.values()) {
			if (pe.code.equals(value)) {
				return pe;
			}
		}
		return defaultE;
	}

}
