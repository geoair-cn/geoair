package cn.geoair.base.data.model.support;

import cn.geoair.base.data.model.GiModelable;
import cn.geoair.base.data.model.annotation.GaModelField;
import java.io.Serializable;

@SuppressWarnings("serial")
public class GirModelKid<ID extends Serializable> implements GiModelable<ID> {

    @GaModelField(isID = true)
    private ID id;

    public GirModelKid() {}

    public GirModelKid(ID id) {
        this.id = id;
    }

    public static <ID extends Serializable> GirModelKid<ID> valueWith(ID id) {
        return new GirModelKid<ID>(id);
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
