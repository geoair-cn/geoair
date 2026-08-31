package cn.geoair.spi.json;

import cn.geoair.base.util.GutilClass;

final class JsonProviderResolver {

    private JsonProviderResolver() {}

    static Json4Gir.JsonUtilSupport resolve() {
        if (GutilClass.isPresent(
                "com.fasterxml.jackson.databind.ObjectMapper",
                GirJacksonJson.class.getClassLoader())) {
            return Json4Gir.JsonUtilSupport.JACKSON;
        } else if (GutilClass.isPresent(
                "com.alibaba.fastjson2.JSON", GirFastJson.class.getClassLoader())) {
            return Json4Gir.JsonUtilSupport.FASTJSON;
        } else if (GutilClass.isPresent(
                "com.alibaba.fastjson.JSON", GirFastJson.class.getClassLoader())) {
            return Json4Gir.JsonUtilSupport.FASTJSON;
        } else if (GutilClass.isPresent(
                "cn.hutool.json.JSON", GirHutoolJson.class.getClassLoader())) {
            return Json4Gir.JsonUtilSupport.HUTOOLS;
        } else if (GutilClass.isPresent(
                "com.google.gson.Gson", GirGsonJson.class.getClassLoader())) {
            return Json4Gir.JsonUtilSupport.GSON;
        }
        return null;
    }
}
