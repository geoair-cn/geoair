package cn.geoair.gtc.base.data.support;

import java.util.HashMap;
import java.util.Map;
import cn.geoair.gtc.base.data.GiGroupType;
import cn.geoair.gtc.base.data.GiType;

public class GirGroupTypeKid extends GirTypeKid implements GiGroupType{

	/**
	 *
	 */
	private static final long serialVersionUID = 3835856895881043495L;


	private static final Map<String, GirGroupTypeKid> typeGroupTypes = new HashMap<>();

	public static GirGroupTypeKid valueOf(GiType  gtcType) {
		if(typeGroupTypes.containsKey( gtcType. gtcTypeId())) {
			return valueFor( gtcType. gtcTypeId());
		}else {
			 GirGroupTypeKid kid = new GirGroupTypeKid();
			kid.setgtcTypeId( gtcType. gtcTypeId());
			kid.setgtcTypeName( gtcType. gtcTypeName());
			typeGroupTypes.put( gtcType. gtcTypeId(), kid);
			return kid;
		}
	}

	public static GirGroupTypeKid valueFor(String  gtcGroupTypeId) {
		return typeGroupTypes.get( gtcGroupTypeId);
	}

}
