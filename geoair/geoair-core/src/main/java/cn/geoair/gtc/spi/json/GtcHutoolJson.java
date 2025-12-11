package cn.geoair.gtc.spi.json;

import cn.hutool.json.JSONUtil;
import cn.geoair.gtc.base.json.GtcJSON;

import java.lang.reflect.Type;

/**
 * @author ：张俊
 * @date ：Created in 2023/8/22 17:08
 * @description： TODO
 */
public class GtcHutoolJson implements GtcJSON {


    private static final long serialVersionUID = 1L;

    private String jsonString = null;
    private Object jsonTarget = null;

    private GtcHutoolJson(String json) {
        this.jsonString = json;
    }

    private GtcHutoolJson(Object object) {
        this.jsonTarget = object;
    }

    public static GtcJSON toJson(Object object) {
        if (object instanceof String) {
            return new GtcHutoolJson((String) object);
        } else {
            return new GtcHutoolJson(object);
        }
    }


    @Override
    public <T> T getByPath(String expression, Class<T> resultType) {

        return JSONUtil.parse(toJSONString()).getByPath(expression, resultType);
    }

    @Override
    public <T> T toBean(Type type, boolean ignoreError) {
        String str = toJSONString();
        cn.hutool.json.JSON json = JSONUtil.parse(str);
        return json.toBean(type, ignoreError);
    }

    @Override
    public String toJSONString() {
        if (jsonString == null) {
            jsonString = JSONUtil.toJsonStr(jsonTarget);
        }
        return jsonString;
    }


}
