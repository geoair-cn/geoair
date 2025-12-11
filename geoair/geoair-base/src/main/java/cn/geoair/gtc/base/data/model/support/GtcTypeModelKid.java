package cn.geoair.gtc.base.data.model.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import cn.geoair.gtc.base.data.model.GiModelType;
import cn.geoair.gtc.base.data.model.GiTypeModelable;

@SuppressWarnings("serial")
public class GtcTypeModelKid<ID extends Serializable> extends GtcModelKid<ID> implements GiTypeModelable<ID> {


	 GtcModelTypeKid gtcModelType;

	@Override
	public GiModelType  gtcModelType() {
		return  gtcModelType;
	}

	public GtcTypeModelKid() {}

	public GtcTypeModelKid(GtcModelTypeKid gtcModelTypeKid, ID id) {
		super(id);
		this. gtcModelType =  gtcModelTypeKid;
	}


	public static <ID extends Serializable> GtcTypeModelKid<ID> valueWith(GtcModelTypeKid modelTypeKid, ID id) {
        return new GtcTypeModelKid<ID>(modelTypeKid,id);
    }

	public static <ID extends Serializable> Collection<GtcTypeModelKid<?>> valuesWith(GtcModelTypeKid modelTypeKid, Set<ID> ids) {

		List<GtcTypeModelKid<?>> list = new ArrayList<>();
		for(ID id : ids) {
			list.add(new GtcTypeModelKid<ID>(modelTypeKid,id));
		}

        return list;
    }

	public GtcModelTypeKid getgtcModelType() {
		return  gtcModelType;
	}

	public void setgtcModelType( GtcModelTypeKid gtcModelType) {
		this. gtcModelType =  gtcModelType;
	}

}
