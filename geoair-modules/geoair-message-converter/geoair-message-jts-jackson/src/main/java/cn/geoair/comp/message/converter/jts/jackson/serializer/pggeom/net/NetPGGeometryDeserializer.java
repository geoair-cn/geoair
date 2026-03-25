package cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.net;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.sql.SQLException;
import net.postgis.jdbc.PGgeometry;
import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 14:33 @description： TODO
 */
public class NetPGGeometryDeserializer extends StdDeserializer<PGgeometry> {

    public NetPGGeometryDeserializer() {
        super(PGgeometry.class);
    }

    @Override
    public PGgeometry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 先反序列化为 JTS Geometry
        Geometry jtsGeometry = ctxt.readValue(p, Geometry.class);
        if (jtsGeometry == null) {
            return null;
        }
        PGgeometry pGobject = new PGgeometry();
        try {
            pGobject.setValue(jtsGeometry.toText());
            pGobject.setType("geometry");
            pGobject.getGeometry().setSrid(jtsGeometry.getSRID());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return pGobject;
    }
}
