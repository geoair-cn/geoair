package cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.net;

import cn.geoair.comp.message.converter.jts.jackson.utils.GirJtsJacksonUtils;
import cn.geoair.map.dynamic.tools.convert.GirPostGisNetTran;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import net.postgis.jdbc.PGgeometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.io.jackson.GeometryAsGeoJSONSerializer;
import org.locationtech.spatial4j.io.jackson.GeometryAsWKTSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// PGGeometry 序列化器：转成 JTS Geometry 再用 JtsModule 序列化
public class NetPGGeometrySerializer extends StdSerializer<PGgeometry> {

    private static final Logger log = LoggerFactory.getLogger(NetPGGeometrySerializer.class);

    public NetPGGeometrySerializer() {
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
            Geometry jtsGeometry = GirPostGisNetTran.toJtsGeometry(pgGeometry);
            if (GirJtsJacksonUtils.jtsToWkt) {
                new GeometryAsWKTSerializer().serialize(jtsGeometry, gen, provider);
            } else {
                new GeometryAsGeoJSONSerializer().serialize(jtsGeometry, gen, provider);
            }
        } catch (Exception e) {
            log.error("PGGeometry 转 JTS Geometry 失败", e);
        }
    }
}
