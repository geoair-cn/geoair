package cn.geoair.base.data.model;

import java.io.Serializable;

import cn.geoair.base.data.GiVisuable;
import cn.geoair.base.data.model.support.GirVisualModelKid;

public interface GiVisualModelable<ID extends Serializable> extends GiModelable<ID>, GiVisuable {

	default GirVisualModelKid<ID> toModelKid() {
		GirVisualModelKid<ID> kid = new GirVisualModelKid<ID>();
		kid.setId(this.id());
		kid.setName(this.display());
		return kid;
	}

}
