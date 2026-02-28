package cn.geoair.gtc.web.data.result;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.result.support.GirResult;

public class GirWebResult<T> extends GirResult<T> implements GiWebResult<T> {

	/**
	 *
	 */
	private static final long serialVersionUID = -5431439929185394126L;

	public GirWebResult() {
	}

	public GirWebResult(Class<T> cls) {
	}

	@GaModelField(text = "跳转地址")
	private String location;// 跳转地址

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String location() {
		return location;
	}

	@Override
	public GiWebResult<T> andLocation(String location) {
		this.setLocation(location);
		return this;
	}

}
