package cn.geoair.base.data.model.support;

import cn.geoair.base.data.model.annotation.GaModelField;

import java.io.Serializable;

@SuppressWarnings("serial")
public class GirVisualTreeModelKid<ID extends Serializable> extends GirVisualModelKid<ID> {

    public GirVisualTreeModelKid() {}

    public GirVisualTreeModelKid(ID id, String name, ID pid) {
        super(id, name);
        this.pid = pid;
    }

    public static <ID extends Serializable> GirVisualTreeModelKid<ID> valueWith(
            ID id, String name, ID pid) {
        return new GirVisualTreeModelKid<ID>(id, name, pid);
    }

    @GaModelField(isParentId = true)
    private ID pid;

    public void setPid(ID parentId) {
        this.pid = parentId;
    }

    public ID getPid() {
        return pid;
    }
}
