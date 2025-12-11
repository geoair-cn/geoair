package cn.geoair.gtc.base.data.page;

import cn.geoair.gtc.base.data.page.support.GtcPageConfig;
import cn.geoair.gtc.base.sp.annotation.GkSP;

/**
 * 分页配置
 * @author Ray
 *
 */

@GkSP(placeHolderClass= { GtcPageConfig.class})
public interface GiPageConfig {


	public GiPagerProvider getPagerProvider();

	public GiPageParamProvider getPageParamProvider();

}
