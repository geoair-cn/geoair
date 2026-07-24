package cn.geoair.comp.message.converter.jts.jackson.serializer.oracle;

import cn.geoair.map.dynamic.tools.convert.GirOracleSpatialTran;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import oracle.sql.STRUCT;

public class OracleSdoGeometrySerializer extends JsonSerializer<STRUCT> {

    @Override
    public void serialize(STRUCT value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String wkt = GirOracleSpatialTran.sdoGeometryToWkt(value);
        gen.writeString(wkt);
    }
}
