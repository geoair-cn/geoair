package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import oracle.sql.BLOB;

import java.io.IOException;

public class OracleBlobDeserializer extends JsonDeserializer<BLOB> {

    @Override
    public BLOB deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        //        try {
        //            byte[] bytes = Base64.getDecoder().decode(parser.getValueAsString());
        //            return BLOB.createBLOB(bytes);
        //        } catch (SQLException e) {
        //            return null;
        //        }
        return null;
    }
}
