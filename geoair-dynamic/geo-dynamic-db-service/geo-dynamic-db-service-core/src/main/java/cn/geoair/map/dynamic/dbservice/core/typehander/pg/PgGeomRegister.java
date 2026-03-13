package cn.geoair.map.dynamic.dbservice.core.typehander.pg;

import cn.geoair.map.dynamic.dbservice.core.typehander.TypeHandlerRegistry;
import cn.geoair.map.dynamic.dbservice.core.typehander.pg.net.NetPgGeomTypeHandler;
import cn.geoair.map.dynamic.dbservice.core.typehander.pg.net.NetPostGisGeomTypeHandler;
import cn.geoair.map.dynamic.dbservice.core.typehander.pg.org.OrgPgGeomTypeHandler;
import cn.geoair.map.dynamic.dbservice.core.typehander.pg.org.OrgPostGisGeomTypeHandler;
import cn.geoair.map.dynamic.tools.convert.GirPostGisTran;
import cn.hutool.core.lang.Singleton;

/**
 * @author ：张逢吉
 * @date ：Created in 13:03 @description： TODO
 */
public class PgGeomRegister {

	public static void register() {
		if (GirPostGisTran.isOrgConvert()) {
			OrgPgGeomTypeHandler.register();
			OrgPostGisGeomTypeHandler.register();
			TypeHandlerRegistry.register(1111, Singleton.get(OrgPgGeomTypeHandler.class));
		}
		if (GirPostGisTran.isNetConvert()) {
			NetPgGeomTypeHandler.register();
			NetPostGisGeomTypeHandler.register();
			TypeHandlerRegistry.register(1112, Singleton.get(NetPgGeomTypeHandler.class));
		}
	}

}
