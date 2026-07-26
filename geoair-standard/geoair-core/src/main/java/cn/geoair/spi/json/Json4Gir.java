package cn.geoair.spi.json;

import cn.geoair.base.json.GirJSON;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

public class Json4Gir {

    private static GiLogger logger = GirLoggerFactory.getLogger(Json4Gir.class);

    public enum JsonUtilSupport {
        FASTJSON,
        GSON,
        JACKSON,
        HUTOOLS
    };

    private static JsonUtilSupport jsonUtil;

    static {
        GirJSON.setProvider(Json4Gir::toJson);
        Json4Gir.setJsonUtilType(JsonProviderResolver.resolve());
        if (jsonUtil == null) {
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
