package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import oracle.sql.NCLOB;

public class OracleNClobDeserializer extends JsonDeserializer<NCLOB> {

    @Override
    public NCLOB deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        try {
            //            String value = parser.getValueAsString();
            //            return NCLOB.createNCLOB(value, null);
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
