package cn.geoair.base.data.page;

import cn.geoair.base.data.page.support.GirPageConfig;
import cn.geoair.base.sp.annotation.GkSP;

/**
 * 分页配置
 *
 * @author Ray
 */
@GkSP(placeHolderClass = {GirPageConfig.class})
public interface GiPageConfig {

    public GiPagerProvider getPagerProvider();

    public GiPageParamProvider getPageParamProvider();
}
