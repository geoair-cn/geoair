package cn.geoair.gtc.base.env.support;

import cn.geoair.gtc.base.def.GkOffice;
import cn.geoair.gtc.base.env.GiEnvironmenter;

public class GtcSystemEnvironmentOffice implements GkOffice<GiEnvironmenter> {



	private GiEnvironmenter env = new GtcSystemEnvironmentOperater();

	@Override
	public GiEnvironmenter getOperater() {
		return env;
	}


}
