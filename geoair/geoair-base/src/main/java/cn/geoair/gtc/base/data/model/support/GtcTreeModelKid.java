package cn.geoair.gtc.base.data.model.support;

import java.io.Serializable;
import java.util.LinkedList;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.model.GiTreeModelable;

@SuppressWarnings("serial")
public class GtcTreeModelKid<ID extends Serializable> extends GtcModelKid<ID> implements GiTreeModelable<ID> {


	public GtcTreeModelKid() {}

	public GtcTreeModelKid(ID id, ID pid) {
		super(id);
		this.pid = pid;
	}

	public static <ID extends Serializable> GtcTreeModelKid<ID> valueWith(ID id, ID pid) {
        return new GtcTreeModelKid<ID>(id,pid);
    }

	@GaModelField(isParentId=true)
	private ID pid;

	public void setPid(ID parentId) {
		this.pid = parentId;
	}

	public ID getPid() {
		return pid;
	}


	public ID parentId() {
		return pid;
	}

	@Override
	public LinkedList<? extends GiTreeModelable<ID>> children() {
		return null;
	}

}
