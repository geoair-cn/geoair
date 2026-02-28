package cn.geoair.gtc.base.data.page.support;

import cn.geoair.gtc.base.data.page.GiPageConfig;
import cn.geoair.gtc.base.data.page.GiPageParamProvider;
import cn.geoair.gtc.base.data.page.GiPagerProvider;
import cn.geoair.gtc.base.sp.GirSpHelper;

/**
 * 分页默认配置
 *
 * @author Ray
 *
 */
public class GirPageConfig implements GiPageConfig {

	@Override
	public GiPagerProvider getPagerProvider() {
		return GirSpHelper.load(GiPagerProvider.class);
	}

	@Override
	public GiPageParamProvider getPageParamProvider() {
		return GirSpHelper.load(GiPageParamProvider.class);
	}

}
