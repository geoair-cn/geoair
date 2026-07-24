package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.sql.SQLException;
import oracle.sql.NCLOB;

public class OracleNClobSerializer extends JsonSerializer<NCLOB> {

    @Override
    public void serialize(NCLOB nclob, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        try {
            if (nclob == null) {
                gen.writeNull();
                return;
            }
            gen.writeString(nclob.stringValue());
        } catch (SQLException e) {
            gen.writeNull();
        }
    }
}
