package cn.geoair.gtc.base.data.page;

import cn.geoair.gtc.base.data.page.support.GtcPagerProvider;
import cn.geoair.gtc.base.sp.annotation.GkSP;

/**
 * 分页结果类型提供者，将由具体功能去实现该接口
 * @author Ray
 **/

@GkSP(placeHolderClass =  GtcPagerProvider.class)
public interface GiPagerProvider{

	public <T> GiPager<T> getPager(Class<T> clazz);

}
