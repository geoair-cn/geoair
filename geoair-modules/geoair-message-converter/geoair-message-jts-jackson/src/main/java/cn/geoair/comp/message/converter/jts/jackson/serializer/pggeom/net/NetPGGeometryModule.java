package cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.net;

import com.fasterxml.jackson.databind.module.SimpleModule;
import net.postgis.jdbc.PGgeometry;

public class NetPGGeometryModule extends SimpleModule {

	public NetPGGeometryModule() {
		super("NetPGGeometryModule");
		// 注册 PGgeometry 类型的序列化器和反序列化器
		addSerializer(PGgeometry.class, new NetPGGeometrySerializer());
		addDeserializer(PGgeometry.class, new NetPGGeometryDeserializer());
	}

}
