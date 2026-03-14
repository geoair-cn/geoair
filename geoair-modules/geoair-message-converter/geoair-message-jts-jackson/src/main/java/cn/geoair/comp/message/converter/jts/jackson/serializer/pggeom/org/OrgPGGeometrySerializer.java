package cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.org;

import cn.geoair.map.dynamic.tools.convert.GirPostGisOrgTran;
import com.bedatadriven.jackson.datatype.jts.serialization.GeometrySerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.locationtech.jts.geom.Geometry;
import org.postgis.PGgeometry;

import java.io.IOException;

import static cn.geoair.base.Gir.log;

// PGGeometry 序列化器：转成 JTS Geometry 再用 JtsModule 序列化
public class OrgPGGeometrySerializer extends StdSerializer<PGgeometry> {

	public OrgPGGeometrySerializer() {
		super(PGgeometry.class);
	}

	@Override
	public void serialize(PGgeometry pgGeometry, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (pgGeometry == null) {
			gen.writeNull();
			return;
		}
		try {
			Geometry jtsGeometry = GirPostGisOrgTran.toJtsGeometry(pgGeometry);
			new GeometrySerializer().serialize(jtsGeometry, gen, provider);
		}
		catch (Exception e) {
			log.error("PGGeometry 转 JTS Geometry 失败", e);
		}

	}

}
