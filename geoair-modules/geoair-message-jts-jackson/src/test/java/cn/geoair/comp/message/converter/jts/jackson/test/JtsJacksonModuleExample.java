package cn.geoair.comp.message.converter.jts.jackson.test;

import cn.geoair.comp.message.converter.jts.jackson.serializer.jts.JtsExtModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Envelope;

/**
 * JtsExtModule 示例
 */
public class JtsJacksonModuleExample {

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JtsExtModule());

        Envelope envelope = new Envelope(116.35, 116.55, 39.85, 40.05);
        String json = objectMapper.writeValueAsString(envelope);

        System.out.println("json = " + json);
    }
}
