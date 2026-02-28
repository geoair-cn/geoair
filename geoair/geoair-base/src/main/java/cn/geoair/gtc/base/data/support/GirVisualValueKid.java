package cn.geoair.gtc.base.data.support;

import cn.geoair.gtc.base.data.GiVisualValuable;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GirVisualValueKid<T> extends GirValueKid<T> implements GiVisualValuable<T> {

	@GaModelField(isDisplay = true)
	private String display;

	public GirVisualValueKid() {
	}

	public GirVisualValueKid(T value, String display) {
		super(value);
		this.display = display;
	}

	public static <T> GirVisualValueKid<T> valueWith(T value, String display) {
		return new GirVisualValueKid<T>(value, display);
	}

	@Override
	public String display() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public String getDisplay() {
		return display;
	}

}
