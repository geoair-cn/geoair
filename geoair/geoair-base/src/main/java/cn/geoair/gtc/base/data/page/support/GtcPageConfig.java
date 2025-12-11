package cn.geoair.gtc.base.data.page.support;

import cn.geoair.gtc.base.data.page.GiPageConfig;
import cn.geoair.gtc.base.data.page.GiPageParamProvider;
import cn.geoair.gtc.base.data.page.GiPagerProvider;
import cn.geoair.gtc.base.sp.GtcSpHelper;

/**
 * 分页默认配置
 * @author Ray
 *
 */
public class GtcPageConfig implements GiPageConfig{


	@Override
	public GiPagerProvider getPagerProvider() {
		return  GtcSpHelper.load(GiPagerProvider.class);
	}

	@Override
	public GiPageParamProvider getPageParamProvider() {
		return  GtcSpHelper.load(GiPageParamProvider.class);
	}


}
