package cn.geoair.gtc.spi.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import cn.geoair.gtc.base.Gtc;
import cn.geoair.gtc.base.bean.GkBeanPath;
import cn.geoair.gtc.base.json.GtcJSON;
import cn.geoair.gtc.base.util.GutilArray;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2023/8/22 17:10
 * @description： TODO
 */
public class GtcJacksonJson implements GtcJSON {


    private static final long serialVersionUID = 1L;


    private String jsonString = null;
    private Object jsonTarget = null;

    private GtcJacksonJson(String json) {
        this.jsonString = json;
    }

    private GtcJacksonJson(Object object) {
        this.jsonTarget = object;
    }

    public static GtcJSON toJson(Object object) {
        if (object instanceof String) {
            return new GtcJacksonJson((String) object);
        } else {
            return new GtcJacksonJson(object);
        }
    }


    private ObjectMapper objectMapper;

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            try {
                objectMapper = Gtc.beans.getBean(ObjectMapper.class);
            } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return jsonString;
    }

}
