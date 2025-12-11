package cn.geoair.gtc.base.env.property.support;

import cn.geoair.gtc.base.def.GkOffice;
import cn.geoair.gtc.base.env.property.GiPropertier;

public class GtcSystemPropertierOffice implements GkOffice<GiPropertier> {


	@Override
	public GiPropertier getOperater() {
		return new GtcSystemPropertyOperater();
	}


}
