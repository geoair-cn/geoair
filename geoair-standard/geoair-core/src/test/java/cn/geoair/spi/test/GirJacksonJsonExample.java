package cn.geoair.spi.test;

import cn.geoair.base.json.GirJSON;
import cn.geoair.spi.json.GirJacksonJson;

/**
 * GirJacksonJson 入口示例
 */
public class GirJacksonJsonExample {

    public static void main(String[] args) {
        GirJSON json = GirJacksonJson.toJson("{\"name\":\"geoair\",\"version\":1}");
        String jsonText = json.toJSONString();
        String name = json.getByPath("name", String.class);

        System.out.println("jsonText = " + jsonText);
        System.out.println("name = " + name);
    }
}
