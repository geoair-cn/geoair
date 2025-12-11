package cn.geoair.gtc.base.data.model.support;

import java.io.Serializable;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GtcVisualTreeModelKid<ID extends Serializable> extends GtcVisualModelKid<ID> {


	public GtcVisualTreeModelKid() {}

	public GtcVisualTreeModelKid(ID id, String name, ID pid) {
		super(id,name);
		this.pid = pid;
	}

	public static <ID extends Serializable> GtcVisualTreeModelKid<ID> valueWith(ID id, String name, ID pid) {
        return new GtcVisualTreeModelKid<ID>(id,name,pid);
    }

	@GaModelField(isParentId=true)
	private ID pid;

	public void setPid(ID parentId) {
		this.pid = parentId;
	}

	public ID getPid() {
		return pid;
	}

}
