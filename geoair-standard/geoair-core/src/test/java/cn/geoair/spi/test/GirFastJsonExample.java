package cn.geoair.spi.test;

import cn.geoair.base.json.GirJSON;
import cn.geoair.spi.json.GirFastJson;

/** GirFastJson 入口示例 */
public class GirFastJsonExample {

    public static void main(String[] args) {
        GirJSON json = GirFastJson.toJson("{\"name\":\"geoair\",\"version\":1}");
        String jsonText = json.toJSONString();
        String name = json.getByPath("name", String.class);

        System.out.println("jsonText = " + jsonText);
        System.out.println("name = " + name);
    }
}
