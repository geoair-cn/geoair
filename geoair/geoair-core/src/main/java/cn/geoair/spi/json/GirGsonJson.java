package cn.geoair.spi.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.geoair.base.bean.GkBeanPath;
import cn.geoair.base.json.GirJSON;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2023/8/22 17:09 @description： TODO
 */
public class GirGsonJson implements GirJSON {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String jsonString = null;

	private Object jsonTarget = null;

	private GirGsonJson(String json) {
		this.jsonString = json;
	}

	private GirGsonJson(Object object) {
		this.jsonTarget = object;
	}

	public static GirJSON toJson(Object object) {
		if (object instanceof String) {
			return new GirGsonJson((String) object);
		}
		else {
			return new GirGsonJson(object);
		}
	}

	@Override
	public <T> T getByPath(String expression, Class<T> resultType) {
		Gson gson = new Gson();
		Map<String, Object> map = gson.fromJson(toJSONString(), new TypeToken<Map<String, Object>>() {
		}.getType());
		Object obj = GkBeanPath.create(expression).get(map);
		return gson.fromJson(gson.toJsonTree(obj), resultType);
	}

	@Override
	public <T> T toBean(Type type, boolean ignoreError) {
		Gson gson = new Gson();
		return gson.fromJson(toJSONString(), type);
	}

	@Override
	public String toJSONString() {
		if (jsonString == null) {
			Gson gson = new Gson();
			jsonString = gson.toJson(jsonTarget);
		}
		return jsonString;
	}

}
