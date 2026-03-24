package cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.net;

import java.io.IOException;

import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bedatadriven.jackson.datatype.jts.serialization.GeometrySerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import cn.geoair.map.dynamic.tools.convert.GirPostGisNetTran;

import net.postgis.jdbc.PGgeometry;

// PGGeometry 序列化器：转成 JTS Geometry 再用 JtsModule 序列化
public class NetPGGeometrySerializer extends StdSerializer<PGgeometry> {

	private static final Logger log = LoggerFactory.getLogger(NetPGGeometrySerializer.class);

	public NetPGGeometrySerializer() {
		super(PGgeometry.class);
	}

	@Override
	public void serialize(PGgeometry pgGeometry, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (pgGeometry == null) {
			gen.writeNull();
			return;
		}
		try {
			Geometry jtsGeometry = GirPostGisNetTran.toJtsGeometry(pgGeometry);
			new GeometrySerializer().serialize(jtsGeometry, gen, provider);
		}
		catch (Exception e) {
			log.error("PGGeometry 转 JTS Geometry 失败", e);
		}

	}

}
