package cn.geoair.gtc.base.data.model;

import java.io.Serializable;

import cn.geoair.gtc.base.data.model.support.GtcVisualModelKid;
import cn.geoair.gtc.base.data.GiVisuable;

public interface GiVisualModelable<ID extends Serializable> extends GiModelable<ID>,GiVisuable {

	default GtcVisualModelKid<ID> toModelKid() {
		 GtcVisualModelKid<ID> kid = new GtcVisualModelKid<ID>();
		kid.setId(this.id());
		kid.setName(this.display());
		return kid;
	}
}
