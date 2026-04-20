package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import oracle.sql.BLOB;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;

public class OracleBlobSerializer extends JsonSerializer<BLOB> {

    @Override
    public void serialize(BLOB blob, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (blob == null) {
            gen.writeNull();
            return;
        }

        try {
            byte[] bytes = blob.getBytes();
            gen.writeString(Base64.getEncoder().encodeToString(bytes));
        } catch ( Exception e) {
            gen.writeNull();
        }
    }
}
