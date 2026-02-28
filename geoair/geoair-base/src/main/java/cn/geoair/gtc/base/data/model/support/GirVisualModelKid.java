package cn.geoair.gtc.base.data.model.support;

import java.io.Serializable;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.GiVisuable;

@SuppressWarnings("serial")
public class GirVisualModelKid<ID extends Serializable> extends GirModelKid<ID> implements GiVisuable {

	@GaModelField(isDisplay = true)
	private String name;

	public GirVisualModelKid() {
	}

	public GirVisualModelKid(ID id, String name) {
		super(id);
		this.name = name;
	}

	public static <ID extends Serializable> GirVisualModelKid<ID> valueWith(ID id, String name) {
		return new GirVisualModelKid<ID>(id, name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
