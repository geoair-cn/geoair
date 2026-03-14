package cn.geoair.comp.db.service.core.typehander.pg.net;

import cn.geoair.comp.db.service.core.typehander.BaseTypeHandler;
import cn.geoair.comp.db.service.core.typehander.TypeHandlerRegistry;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.hutool.core.lang.Singleton;
import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： TODO
 */
public class NetPgGeomTypeHandler extends BaseTypeHandler<String> {

	public static void register() {
		TypeHandlerRegistry.register(net.postgis.jdbc.PGgeometry.class, Singleton.get(NetPgGeomTypeHandler.class));
	}

	@Override
	public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
		return null;
	}

	@Override
	public String getResult(Entity entity, String columnName) {
		Object obj = entity.getObj(columnName);
		return GirAdvTools.getFormatOpt().pgGeometryToWkt(obj, true);
	}

	@Override
	public String getResult(ResultSet resultSet, String columnName) {
		Object obj = null;
		try {
			obj = resultSet.getObject(columnName);
		}
		catch (SQLException throwables) {
			throwables.printStackTrace();
		}

		return GirAdvTools.getFormatOpt().pgGeometryToWkt(obj, true);
	}

	@Override
	public String getResult(ResultSet resultSet, Integer columnIndex) {
		Object obj = null;
		try {
			obj = resultSet.getObject(columnIndex);
		}
		catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return GirAdvTools.getFormatOpt().pgGeometryToWkt(obj, true);
	}

	@Override
	public String getResult(Map<String, Object> row, String columnName) {
		Object obj = null;
		obj = row.get(columnName);
		return GirAdvTools.getFormatOpt().pgGeometryToWkt(obj, true);
	}

	@Override
	public String getResult(Object obj) {
		return GirAdvTools.getFormatOpt().pgGeometryToWkt(obj, true);
	}

}
