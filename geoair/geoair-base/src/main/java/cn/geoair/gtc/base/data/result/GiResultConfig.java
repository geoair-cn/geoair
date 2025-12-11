package cn.geoair.gtc.base.data.result;

import cn.geoair.gtc.base.data.result.support.GtcResultConfig;
import cn.geoair.gtc.base.sp.annotation.GkSP;

/**
 * 结果集配置
 * @author Ray
 *
 */

@GkSP(placeHolderClass= GtcResultConfig.class)
public interface GiResultConfig {


	/**
	 * 获取结果集对象
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public <T> GiResult<T> getResult(Class<T> clazz);

}
