package cn.geoair.gtc.base.data.support;

import java.util.HashMap;
import java.util.Map;
import cn.geoair.gtc.base.data.GiGroupType;
import cn.geoair.gtc.base.data.GiType;

public class GtcGroupTypeKid extends  GtcTypeKid implements GiGroupType{

	/**
	 *
	 */
	private static final long serialVersionUID = 3835856895881043495L;


	private static final Map<String, GtcGroupTypeKid> typeGroupTypes = new HashMap<>();

	public static GtcGroupTypeKid valueOf(GiType  gtcType) {
		if(typeGroupTypes.containsKey( gtcType. gtcTypeId())) {
			return valueFor( gtcType. gtcTypeId());
		}else {
			 GtcGroupTypeKid kid = new GtcGroupTypeKid();
			kid.setgtcTypeId( gtcType. gtcTypeId());
			kid.setgtcTypeName( gtcType. gtcTypeName());
			typeGroupTypes.put( gtcType. gtcTypeId(), kid);
			return kid;
		}
	}

	public static GtcGroupTypeKid valueFor(String  gtcGroupTypeId) {
		return typeGroupTypes.get( gtcGroupTypeId);
	}

}
