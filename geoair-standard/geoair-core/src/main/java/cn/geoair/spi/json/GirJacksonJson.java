package cn.geoair.spi.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import cn.geoair.base.Gir;
import cn.geoair.base.bean.GkBeanPath;
import cn.geoair.base.json.GirJSON;
import cn.geoair.base.util.GutilArray;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2023/8/22 17:10 @description： TODO
 */
public class GirJacksonJson implements GirJSON {

	private static final long serialVersionUID = 1L;

	private String jsonString = null;

	private Object jsonTarget = null;

	private GirJacksonJson(String json) {
		this.jsonString = json;
	}

	private GirJacksonJson(Object object) {
		this.jsonTarget = object;
	}

	public static GirJSON toJson(Object object) {
		if (object instanceof String) {
			return new GirJacksonJson((String) object);
		}
		else {
			return new GirJacksonJson(object);
		}
	}

	private ObjectMapper objectMapper;

	private ObjectMapper getObjectMapper() {
		if (objectMapper == null) {
			try {
				objectMapper = Gir.beans.getBean(ObjectMapper.class);
			}
			catch (Exception e) {
			}
		}

		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}
		return objectMapper;
	}

	@Override
	public <T> T getByPath(String expression, Class<T> resultType) {
		ObjectMapper mapper = getObjectMapper();
		JsonNode jsonNode;
		try {
			jsonNode = mapper.readTree(toJSONString());
			List<String> paths = GkBeanPath.create(expression).getPatternParts();
			String path = "/" + GutilArray.join(paths.toArray(), "/");
			return mapper.readValue(mapper.writeValueAsString(jsonNode.at(path)), resultType);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public <T> T toBean(Type type, boolean ignoreError) {
		ObjectMapper mapper = getObjectMapper();
		JavaType javaType = TypeFactory.defaultInstance().constructType(type);
		try {
			return mapper.readValue(toJSONString(), javaType);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toJSONString() {
		if (jsonString == null) {
			ObjectMapper mapper = getObjectMapper();
			try {
				jsonString = mapper.writeValueAsString(jsonTarget);
			}
			catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
		return jsonString;
	}

}
