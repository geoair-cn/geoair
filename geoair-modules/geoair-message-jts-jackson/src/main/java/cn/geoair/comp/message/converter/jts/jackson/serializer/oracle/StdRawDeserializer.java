package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Base64;

public class StdRawDeserializer extends JsonDeserializer<byte[]> {
    @Override
    public byte[] deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.isEmpty()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(value);
    }
}
