package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.io.Reader;
import java.sql.NClob;
import java.sql.SQLException;

public class StdNClobSerializer extends JsonSerializer<NClob> {
    @Override
    public void serialize(NClob nclob, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (nclob == null) {
            gen.writeNull();
            return;
        }

        try (Reader reader = nclob.getCharacterStream()) {
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
