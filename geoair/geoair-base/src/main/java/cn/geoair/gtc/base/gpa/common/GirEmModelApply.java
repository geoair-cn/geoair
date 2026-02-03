package cn.geoair.gtc.base.gpa.common;

import cn.geoair.gtc.base.data.GiVisualValuable;

public enum GirEmModelApply implements GiVisualValuable<String> {

	INSERT("新增"),DELETE("删除"),UPDATE("修改"),SELECT("查询");

	//@GaModelField(isID = true)
	private String code;

	 GirEmModelApply(String code) {
		this.code = code;
	}

	public String getCode() {
		return this.name();
	}

	@Override
	public String display() {
		return this.code;
	}

	@Override
	public String value() {
		return this.name();
	}

}
