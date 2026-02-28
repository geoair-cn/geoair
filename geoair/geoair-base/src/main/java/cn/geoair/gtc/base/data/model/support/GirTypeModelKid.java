package cn.geoair.gtc.base.data.model.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import cn.geoair.gtc.base.data.model.GiModelType;
import cn.geoair.gtc.base.data.model.GiTypeModelable;

@SuppressWarnings("serial")
public class GirTypeModelKid<ID extends Serializable> extends GirModelKid<ID> implements GiTypeModelable<ID> {

	GirModelTypeKid gtcModelType;

	@Override
	public GiModelType gtcModelType() {
		return gtcModelType;
	}

	public GirTypeModelKid() {
	}

	public GirTypeModelKid(GirModelTypeKid gtcModelTypeKid, ID id) {
		super(id);
		this.gtcModelType = gtcModelTypeKid;
	}

	public static <ID extends Serializable> GirTypeModelKid<ID> valueWith(GirModelTypeKid modelTypeKid, ID id) {
		return new GirTypeModelKid<ID>(modelTypeKid, id);
	}

	public static <ID extends Serializable> Collection<GirTypeModelKid<?>> valuesWith(GirModelTypeKid modelTypeKid,
			Set<ID> ids) {

		List<GirTypeModelKid<?>> list = new ArrayList<>();
		for (ID id : ids) {
			list.add(new GirTypeModelKid<ID>(modelTypeKid, id));
		}

		return list;
	}

	public GirModelTypeKid getgtcModelType() {
		return gtcModelType;
	}

	public void setgtcModelType(GirModelTypeKid gtcModelType) {
		this.gtcModelType = gtcModelType;
	}

}
