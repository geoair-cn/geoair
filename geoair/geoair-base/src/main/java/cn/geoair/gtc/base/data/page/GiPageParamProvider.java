package cn.geoair.gtc.base.data.page;

import cn.geoair.gtc.base.sp.annotation.GkSP;

/**
 *
 * 分页参数提供者，实现者去实现该接口
 *
 * @author Ray
 **/

@GkSP()
public interface GiPageParamProvider {

	public GiPageParam getPageParam();

}
