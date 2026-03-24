package cn.geoair.comp.message.converter.jts.jackson.serializer.pggeom.org;

import org.postgis.PGgeometry;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class OrgPGGeometryModule extends SimpleModule {

	public OrgPGGeometryModule() {
		super("OrgPGGeometryModule"); // 给 Module 命名，
		// 注册 PGgeometry 类型的序列化器和反序列化器
		addSerializer(PGgeometry.class, new OrgPGGeometrySerializer());
		addDeserializer(PGgeometry.class, new OrgPGGeometryDeserializer());
	}

}
