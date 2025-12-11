package cn.geoair.gtc.web.data.result;

import cn.geoair.gtc.base.data.result.GiResult;
import cn.geoair.gtc.base.data.result.GiResultConfig;

/**
 * Web结果集默认配置
 * @author Ray
 *
 */
public class GtcWebResultConfig implements GiResultConfig{

	@Override
	public <T> GiResult<T> getResult(Class<T> clazz) {
		return new GtcWebResult<T>(clazz);
	}



}
