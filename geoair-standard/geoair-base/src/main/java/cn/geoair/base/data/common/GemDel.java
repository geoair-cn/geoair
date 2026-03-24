package cn.geoair.base.data.common;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.model.annotation.GaModelField;

public enum GemDel implements GiVisualValuable<Integer> {

	已删除(1), 未删除(0);

	@GaModelField(isID = true)
	private Integer code;

	GemDel(Integer code) {
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

	public static GemDel findByValue(Integer value, GemDel defaultE) {
		for (GemDel pe : GemDel.values()) {
			if (pe.code.equals(value)) {
				return pe;
			}
		}
		return defaultE;
	}

	public static GemDel findByValue(int value, GemDel defaultE) {
		for (GemDel pe : GemDel.values()) {
			if (pe.code.equals(value)) {
				return pe;
			}
		}
		return defaultE;
	}

}
