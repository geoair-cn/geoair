package cn.geoair.gtc.base.data.support;

import cn.geoair.gtc.base.data.GiValuable;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GtcValueKid<T> implements GiValuable<T>{

	@GaModelField(isID=true)
	private T value;

	public GtcValueKid() {}

	public GtcValueKid(T value) {
		this.value = value;
	}

	public static<T> GtcValueKid<T> valueWith(T value) {
		return new GtcValueKid<T>(value);
	}


	@Override
	public T value() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public T getValue() {
		return value;
	}
}
