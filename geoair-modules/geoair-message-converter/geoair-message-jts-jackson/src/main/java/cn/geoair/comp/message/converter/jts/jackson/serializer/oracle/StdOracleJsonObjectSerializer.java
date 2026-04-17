package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import oracle.sql.json.OracleJsonObject;

import java.io.IOException;

public class StdOracleJsonObjectSerializer extends JsonSerializer<OracleJsonObject> {
    @Override
    public void serialize(OracleJsonObject jsonObject, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (jsonObject == null) {
            gen.writeNull();
            return;
        }
        // 直接输出 JSON 字符串，无需手动解析
        gen.writeRawValue(jsonObject.toString());
    }
}
