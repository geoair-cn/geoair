package cn.geoair.base.data.result;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.model.annotation.GaModelField;

public enum GirEmAlertType implements GiVisualValuable<Integer> {

	不弹框0(0), 无需关闭的提示1(1), 需要关闭的提示2(2), 无需关闭的错误3(3), 需要关闭的错误4(4), 弹出确认警告5(5),;

	@GaModelField(isID = true)
	private Integer code;

	GirEmAlertType(Integer code) {
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

}
