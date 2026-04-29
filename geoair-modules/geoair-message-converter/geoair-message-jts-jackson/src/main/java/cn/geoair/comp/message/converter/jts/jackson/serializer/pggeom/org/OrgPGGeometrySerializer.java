package cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.org;

import static cn.geoair.base.Gir.log;

import cn.geoair.comp.message.converter.jts.jackson.utils.GirJacksonUtils;
import cn.geoair.map.dynamic.tools.convert.GirPostGisOrgTran;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer;
import org.locationtech.spatial4j.io.jackson.GeometryAsWKTSerializer;
import org.postgis.PGgeometry;

// PGGeometry 序列化器：转成 JTS Geometry 再用 JtsModule 序列化
public class OrgPGGeometrySerializer extends StdSerializer<PGgeometry> {

    public OrgPGGeometrySerializer() {
        super(PGgeometry.class);
    }

    @Override
    public void serialize(PGgeometry pgGeometry, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (pgGeometry == null) {
            gen.writeNull();
            return;
        }
        try {
            Geometry jtsGeometry = GirPostGisOrgTran.toJtsGeometry(pgGeometry);
            if (GirJacksonUtils.jtsToWkt) {
                new GeometryAsWKTSerializer().serialize(jtsGeometry, gen, provider);
            } else {
                new GeometryAsGeoJSONSerializer().serialize(jtsGeometry, gen, provider);
            }
        } catch (Exception e) {
            log.error("PGGeometry 转 JTS Geometry 失败", e);
        }
    }
}
