package cn.geoair.gtc.spi.json;

import cn.geoair.gtc.base.json.GtcJSON;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.gtc.base.log.GiLoger;
import cn.geoair.gtc.base.log.GtcLoger;
import cn.geoair.gtc.base.util.GutilClass;

public class Json4Gtc {


	private static GiLoger logger =  GtcLoger.getLoger(Json4Gtc.class);

	public enum JsonUtilSupport {FASTJSON, GSON,JACKSON, HUTOOLS};


	private static JsonUtilSupport jsonUtil;

	static {
		if(GutilClass.isPresent("com.alibaba.fastjson.JSON", GtcFastJson.class.getClassLoader())) {
			Json4Gtc.setJsonUtilType(JsonUtilSupport.FASTJSON);
		}else if(GutilClass.isPresent("com.fasterxml.jackson.databind.ObjectMapper",GtcJacksonJson.class.getClassLoader())) {
			Json4Gtc.setJsonUtilType(JsonUtilSupport.JACKSON);
		}else if(GutilClass.isPresent("cn.hutool.json.JSON", GtcHutoolJson.class.getClassLoader())) {
			Json4Gtc.setJsonUtilType(JsonUtilSupport.HUTOOLS);
		}else if(GutilClass.isPresent("com.google.gson.Gson", GtcGsonJson.class.getClassLoader())) {
			Json4Gtc.setJsonUtilType(JsonUtilSupport.GSON);
		}else {
			logger.warn("未找到合适的json转换工具");
		}
	}


	public static void setJsonUtilType(JsonUtilSupport jsonUtilSupport) {
		jsonUtil = jsonUtilSupport;
	}


	@GaMethodHandImpl(implClass= GtcJSON.class,implMethod="toJson",type=ImplType.expectfirst)
	public static GtcJSON toJson(Object object) {
		switch (jsonUtil) {
		case JACKSON:
			return GtcJacksonJson.toJson(object);
		case FASTJSON:
			return GtcFastJson.toJson(object);
		case GSON:
			return GtcGsonJson.toJson(object);
		case HUTOOLS:
			return GtcHutoolJson.toJson(object);
		default:
			return null;
		}
	}



}
