package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Base64;

public class StdRawSerializer extends JsonSerializer<byte[]> {
    @Override
    public void serialize(byte[] raw, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (raw == null || raw.length == 0) {
            gen.writeNull();
            return;
        }
        // RAW 二进制转 Base64 字符串
        gen.writeString(Base64.getEncoder().encodeToString(raw));
    }
}
