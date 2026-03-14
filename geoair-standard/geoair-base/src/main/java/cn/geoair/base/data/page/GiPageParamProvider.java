package cn.geoair.base.data.page;

import cn.geoair.base.sp.annotation.GkSP;

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
