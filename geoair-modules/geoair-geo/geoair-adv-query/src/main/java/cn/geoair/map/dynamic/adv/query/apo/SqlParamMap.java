package cn.geoair.map.dynamic.adv.query.apo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.geoair.map.dynamic.adv.query.result.OptNullGeomAndBasicTypeFromObjectGetter;

import cn.hutool.core.lang.Pair;

/**
 * @author ：张逢吉
 * @date ：Created in 17:47 @description： SqlParam的一个map对象，主要是提供一个比较方便的API进行set值
 */
public class SqlParamMap extends LinkedHashMap<String, Object>
		implements OptNullGeomAndBasicTypeFromObjectGetter, Serializable {

	public static SqlParamMap of() {
		return new SqlParamMap();
	}

	public SqlParamMap addAll(Map<String, Object> all) {
		super.putAll(all);
		return this;
	}

	public SqlParamMap addOne(String key, Object value) {
		super.put(key, value);
		return this;
	}

	public SqlParamMap addPair(Pair<String, Object> pair) {
		super.put(pair.getKey(), pair.getValue());
		return this;
	}

	@Override
	public Object getObj(String key, Object defaultValue) {
		Object o = super.get(key);
		if (o == null) {
			return defaultValue;
		}
		return o;
	}

}
