package cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.org;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.sql.SQLException;
import org.locationtech.jts.geom.Geometry;
import org.postgis.PGgeometry;

/**
 * @author ：张逢吉
 * @date ：Created in 14:33 @description： TODO
 */
public class OrgPGGeometryDeserializer extends StdDeserializer<PGgeometry> {

    public OrgPGGeometryDeserializer() {
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
