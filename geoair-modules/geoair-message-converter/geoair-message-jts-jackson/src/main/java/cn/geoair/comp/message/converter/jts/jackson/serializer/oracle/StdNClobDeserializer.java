package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLException;

public class StdNClobDeserializer extends JsonDeserializer<NClob> {
    @Override
    public NClob deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null) return null;

        try {
            Connection conn = null; // 注意：反序列化NClob一般需要连接，这里仅做兼容
            NClob nclob = conn.createNClob();
            nclob.setString(1, value);
            return nclob;
        } catch (SQLException e) {
            return null;
        }
    }
}
