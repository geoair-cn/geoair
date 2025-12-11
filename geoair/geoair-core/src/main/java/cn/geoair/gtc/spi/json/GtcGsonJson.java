package cn.geoair.gtc.spi.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.geoair.gtc.base.bean.GkBeanPath;
import cn.geoair.gtc.base.json.GtcJSON;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2023/8/22 17:09
 * @description： TODO
 */
public class GtcGsonJson implements GtcJSON {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    private String jsonString = null;
    private Object jsonTarget = null;

    private GtcGsonJson(String json) {
        this.jsonString = json;
    }
    private GtcGsonJson(Object object) {
        this.jsonTarget = object;
    }

    public static GtcJSON toJson(Object object) {
        if(object instanceof String) {
            return new GtcGsonJson((String)object);
        }else {
            return new GtcGsonJson(object);
        }
    }


    @Override
    public <T> T getByPath(String expression, Class<T> resultType) {
        Gson gson = new Gson();
        Map<String,Object> map = gson.fromJson(toJSONString(),new TypeToken<Map<String,Object>>(){ }.getType());
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
        if(jsonString == null) {
            Gson gson = new Gson();
            jsonString = gson.toJson(jsonTarget);
        }
        return jsonString;
    }

}
