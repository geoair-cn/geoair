package cn.geoair.spi.json;

import java.lang.reflect.Type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.geoair.base.bean.GkBeanPath;
import cn.geoair.base.json.GirJSON;

/**
 * @author ：张俊
 * @date ：Created in 2023/8/22 17:07 @description： TODO
 */
public class GirFastJson implements GirJSON {

	private static final long serialVersionUID = 1L;

	private String jsonString = null;

	private Object jsonTarget = null;

	protected GirFastJson(String json) {
		this.jsonString = json;
	}

	protected GirFastJson(Object object) {
		this.jsonTarget = object;
	}

	public static GirJSON toJson(Object object) {
		if (object instanceof String) {
			return new GirFastJson((String) object);
		}
		else {
			return new GirFastJson(object);
		}
	}

	@Override
	public <T> T getByPath(String expression, Class<T> resultType) {

		Object json = JSON.parse(toJSONString());
		Object obj = GkBeanPath.create(expression).get(json);

		return JSON.toJavaObject((JSON) obj, resultType);
	}

	@Override
	public <T> T toBean(Type type, boolean ignoreError) {
		return JSON.parseObject(toJSONString(), type);
	}

	@Override
	public String toJSONString() {
		if (jsonString == null) {
			jsonString = JSONObject.toJSONString(jsonTarget, SerializerFeature.MapSortField,
					SerializerFeature.WriteMapNullValue);
		}
		return jsonString;
	}

}
