package cn.geoair.spi.json;

import cn.geoair.base.json.GirJSON;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilClass;

public class Json4Gir {

	private static GiLogger logger = GirLogger.getLoger(Json4Gir.class);

	public enum JsonUtilSupport {

		FASTJSON, GSON, JACKSON, HUTOOLS

	}

	;

	private static JsonUtilSupport jsonUtil;

	static {
		if (GutilClass.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
				GirJacksonJson.class.getClassLoader())) {
			Json4Gir.setJsonUtilType(JsonUtilSupport.JACKSON);
		}
		else if (GutilClass.isPresent("com.alibaba.fastjson2.JSON", GirFastJson.class.getClassLoader())) {
			Json4Gir.setJsonUtilType(JsonUtilSupport.FASTJSON);
		}
		else if (GutilClass.isPresent("com.alibaba.fastjson.JSON", GirFastJson.class.getClassLoader())) {
			Json4Gir.setJsonUtilType(JsonUtilSupport.FASTJSON);
		}
		else if (GutilClass.isPresent("cn.hutool.json.JSON", GirHutoolJson.class.getClassLoader())) {
			Json4Gir.setJsonUtilType(JsonUtilSupport.HUTOOLS);
		}
		else if (GutilClass.isPresent("com.google.gson.Gson", GirGsonJson.class.getClassLoader())) {
			Json4Gir.setJsonUtilType(JsonUtilSupport.GSON);
		}
		else {
			logger.warn("未找到合适的json转换工具");
		}
	}

	public static void setJsonUtilType(JsonUtilSupport jsonUtilSupport) {
		jsonUtil = jsonUtilSupport;
	}

	@GaMethodHandImpl(implClass = GirJSON.class, implMethod = "toJson", type = ImplType.expectfirst)
	public static GirJSON toJson(Object object) {
		switch (jsonUtil) {
		case JACKSON:
			return GirJacksonJson.toJson(object);
		case FASTJSON:
			return GirFastJson.toJson(object);
		case GSON:
			return GirGsonJson.toJson(object);
		case HUTOOLS:
			return GirHutoolJson.toJson(object);
		default:
			return null;
		}
	}

}
