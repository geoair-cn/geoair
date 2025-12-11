package cn.geoair.gtc.base.data.model.support;

import java.io.Serializable;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.model.GiModelable;

@SuppressWarnings("serial")
public class GtcModelKid<ID extends Serializable> implements GiModelable<ID> {

	@GaModelField(isID=true)
	private ID id;


	public GtcModelKid() {}

	public GtcModelKid(ID id) {
		this.id = id;
	}

	public static <ID extends Serializable> GtcModelKid<ID> valueWith(ID id) {
        return new GtcModelKid<ID>(id);
    }


	public void setId(ID id) {
		this.id = id;
	}


	@Override
	public ID id() {
		return id;
	}

	public ID getId() {
		return id;
	}

}
