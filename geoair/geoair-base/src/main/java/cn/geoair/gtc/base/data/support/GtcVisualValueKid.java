package cn.geoair.gtc.base.data.support;


import cn.geoair.gtc.base.data.GiVisualValuable;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GtcVisualValueKid<T> extends  GtcValueKid<T> implements GiVisualValuable<T> {


	@GaModelField(isDisplay=true)
	private String display;

	public GtcVisualValueKid() {}

	public GtcVisualValueKid(T value, String display) {
		super(value);
		this.display = display;
	}

	public static<T> GtcVisualValueKid<T> valueWith(T value, String display) {
		return new GtcVisualValueKid<T>(value,display);
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
