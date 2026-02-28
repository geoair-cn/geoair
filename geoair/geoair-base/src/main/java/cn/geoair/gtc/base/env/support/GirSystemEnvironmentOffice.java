package cn.geoair.gtc.base.env.support;

import cn.geoair.gtc.base.def.GkOffice;
import cn.geoair.gtc.base.env.GiEnvironmenter;

public class GirSystemEnvironmentOffice implements GkOffice<GiEnvironmenter> {

	private GiEnvironmenter env = new GirSystemEnvironmentOperater();

	@Override
	public GiEnvironmenter getOperater() {
		return env;
	}

}
