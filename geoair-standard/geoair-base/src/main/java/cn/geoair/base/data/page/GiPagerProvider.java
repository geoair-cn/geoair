package cn.geoair.base.data.page;

import cn.geoair.base.data.page.support.GirPagerProvider;
import cn.geoair.base.sp.annotation.GkSP;

/**
 * 分页结果类型提供者，将由具体功能去实现该接口
 *
 * @author Ray
 */
@GkSP(placeHolderClass = GirPagerProvider.class)
public interface GiPagerProvider {

    public <T> GiPager<T> getPager(Class<T> clazz);
}
