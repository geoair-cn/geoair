package cn.geoair.gtc.base.data.support;

import cn.geoair.gtc.base.data.GiValuable;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GirValueKid<T> implements GiValuable<T> {

	@GaModelField(isID = true)
	private T value;

	public GirValueKid() {
	}

	public GirValueKid(T value) {
		this.value = value;
	}

	public static <T> GirValueKid<T> valueWith(T value) {
		return new GirValueKid<T>(value);
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
