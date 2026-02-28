package cn.geoair.gtc.base.data;

import cn.geoair.gtc.base.data.support.GirGroupTypeKid;
import cn.geoair.gtc.base.util.GutilStr;

public interface GiGroupType extends GiType {

	default GirGroupTypeKid toGroupTypeKid() {

		if (GutilStr.isNotEmpty(this.gtcTypeId())) {
			GirGroupTypeKid kid = GirGroupTypeKid.valueFor(this.gtcTypeId());
			if (kid != null) {
				return kid;
			}
		}
		return null;
	}

}
