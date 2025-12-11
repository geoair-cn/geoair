package cn.geoair.gtc.base.data.model.support;

import java.io.Serializable;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.GiVisuable;

@SuppressWarnings("serial")
public class GtcVisualModelKid<ID extends Serializable> extends GtcModelKid<ID> implements GiVisuable {

	@GaModelField(isDisplay=true)
	private String name;


	public GtcVisualModelKid() {}

	public GtcVisualModelKid(ID id, String name) {
		super(id);
		this.name = name;
	}

	public static <ID extends Serializable> GtcVisualModelKid<ID> valueWith(ID id, String name) {
        return new GtcVisualModelKid<ID>(id,name);
    }


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}

}
