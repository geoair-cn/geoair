package cn.geoair.spi.test;

import cn.geoair.spi.json.GirJacksonJson;
import cn.geoair.base.json.GirJSON;

import java.util.HashMap;
import java.util.Map;

/**
 * GirJacksonJson 最小示例
 */
public class GirJacksonJsonExample {

    public static void main(String[] args) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "geoair");
        payload.put("version", "J17-dev-SNAPSHOT");

        GirJSON json = GirJacksonJson.toJson(payload);
        String text = json.toJSONString();

        System.out.println("json = " + text);
    }
}
