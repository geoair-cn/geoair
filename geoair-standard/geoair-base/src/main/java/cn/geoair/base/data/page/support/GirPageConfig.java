package cn.geoair.base.data.page.support;

import cn.geoair.base.data.page.GiPageConfig;
import cn.geoair.base.data.page.GiPageParamProvider;
import cn.geoair.base.data.page.GiPagerProvider;
import cn.geoair.base.sp.GirSpHelper;

/**
 * 分页默认配置
 *
 * @author Ray
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
