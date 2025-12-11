package cn.geoair.gtc.base.data;

import cn.geoair.gtc.base.data.support.GtcGroupTypeKid;
import cn.geoair.gtc.base.util.GutilStr;

public interface GiGroupType extends GiType{


	default GtcGroupTypeKid toGroupTypeKid() {

		if(GutilStr.isNotEmpty(this. gtcTypeId())) {
			 GtcGroupTypeKid kid =  GtcGroupTypeKid.valueFor(this. gtcTypeId());
			if(kid != null) {
				return kid;
			}
		}
		return null;
	}

}
