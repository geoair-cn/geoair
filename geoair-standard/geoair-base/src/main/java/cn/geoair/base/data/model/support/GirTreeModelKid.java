package cn.geoair.base.data.model.support;

import java.io.Serializable;
import java.util.LinkedList;

import cn.geoair.base.data.model.GiTreeModelable;
import cn.geoair.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GirTreeModelKid<ID extends Serializable> extends GirModelKid<ID> implements GiTreeModelable<ID> {

	public GirTreeModelKid() {
	}

	public GirTreeModelKid(ID id, ID pid) {
		super(id);
		this.pid = pid;
	}

	public static <ID extends Serializable> GirTreeModelKid<ID> valueWith(ID id, ID pid) {
		return new GirTreeModelKid<ID>(id, pid);
	}

	@GaModelField(isParentId = true)
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
