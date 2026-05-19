package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Base64;

public class StdBlobSerializer extends JsonSerializer<Blob> {
    @Override
    public void serialize(Blob blob, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (blob == null) {
            gen.writeNull();
            return;
        }

        try {
            byte[] bytes = blob.getBytes(1, (int) blob.length());
            gen.writeString(Base64.getEncoder().encodeToString(bytes));
        } catch (SQLException e) {
            gen.writeNull();
        }
    }
}
