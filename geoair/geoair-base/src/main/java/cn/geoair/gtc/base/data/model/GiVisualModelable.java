package cn.geoair.gtc.base.data.model;

import java.io.Serializable;

import cn.geoair.gtc.base.data.model.support.GirVisualModelKid;
import cn.geoair.gtc.base.data.GiVisuable;

public interface GiVisualModelable<ID extends Serializable> extends GiModelable<ID>,GiVisuable {

	default GirVisualModelKid<ID> toModelKid() {
		 GirVisualModelKid<ID> kid = new GirVisualModelKid<ID>();
		kid.setId(this.id());
		kid.setName(this.display());
		return kid;
	}
}
