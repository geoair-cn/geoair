package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;

public class StdClobSerializer extends JsonSerializer<Clob> {
    @Override
    public void serialize(Clob clob, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (clob == null) {
            gen.writeNull();
            return;
        }

        try (Reader reader = clob.getCharacterStream()) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            gen.writeString(sb.toString());
        } catch (SQLException e) {
            gen.writeNull();
        }
    }
}
