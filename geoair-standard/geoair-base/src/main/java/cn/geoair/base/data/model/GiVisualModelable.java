package cn.geoair.base.data.model;

import cn.geoair.base.data.GiVisuable;
import cn.geoair.base.data.model.support.GirVisualModelKid;

import java.io.Serializable;

public interface GiVisualModelable<ID extends Serializable> extends GiModelable<ID>, GiVisuable {

    default GirVisualModelKid<ID> toModelKid() {
        GirVisualModelKid<ID> kid = new GirVisualModelKid<ID>();
        kid.setId(this.id());
        kid.setName(this.display());
        return kid;
    }
}
