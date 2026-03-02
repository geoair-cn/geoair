package cn.geoair.base.env.property.support;

import cn.geoair.base.def.GkOffice;
import cn.geoair.base.env.property.GiPropertier;

public class GirSystemPropertierOffice implements GkOffice<GiPropertier> {

	@Override
	public GiPropertier getOperater() {
		return new GirSystemPropertyOperater();
	}

}
