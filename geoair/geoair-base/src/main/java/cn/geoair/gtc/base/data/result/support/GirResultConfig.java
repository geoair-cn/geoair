package cn.geoair.gtc.base.data.result.support;

import cn.geoair.gtc.base.data.result.GiResult;
import cn.geoair.gtc.base.data.result.GiResultConfig;

/**
 * 结果集默认配置
 * @author Ray
 *
 */
public class GirResultConfig implements GiResultConfig{

	/*
	@Override
	public <T>  gtcResult<T> getResult(Class<T> clazz) {
		return new  gtcSimpleResult<T>(clazz);
	}
	@Override
	public <T>  gtcResult<T> getSuccessResult(Class<T> clazz) {
		return new  gtcSimpleResult<T>(clazz).forSuccess();
	}
	*/
	@Override
	public <T> GiResult<T> getResult(Class<T> clazz) {
		return new GirResult<T>(clazz);
	}



}
