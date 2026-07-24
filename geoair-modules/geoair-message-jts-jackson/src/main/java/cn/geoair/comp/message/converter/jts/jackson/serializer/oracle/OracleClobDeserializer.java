package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import oracle.sql.CLOB;

public class OracleClobDeserializer extends JsonDeserializer<CLOB> {

    @Override
    public CLOB deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        //        try {
        //            return CLOB.createCLOB(parser.getValueAsString(), null);
        //        } catch (SQLException e) {
        //            return null;
        //        }
        return null;
    }
}
