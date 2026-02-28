package cn.geoair.gtc.web.data.result;

import cn.geoair.gtc.base.data.result.GiResult;

/**
 * @author Ray
 * @param <T>
 */
public interface GiWebResult<T> extends GiResult<T> {

	/**
	 * 获取重定向Location
	 *
	 */
	public String location();

	/**
	 * 设置重定向Location
	 *
	 */
	public GiWebResult<T> andLocation(String location);

	public static <K> GiWebResult<K> withLocation(String location) {
		return GiWebResult.<K>getResult(null).andLocation(location);
	}

	/**
	 * 获取结果对象
	 *
	 */
	public static <K> GiWebResult<K> getResult(Class<K> resultType) {
		return (GiWebResult<K>) GiResult.<K>getResult(resultType);
	}

}
