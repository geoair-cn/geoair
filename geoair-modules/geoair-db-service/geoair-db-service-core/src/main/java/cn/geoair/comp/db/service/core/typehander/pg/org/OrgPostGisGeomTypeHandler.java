package cn.geoair.comp.db.service.core.typehander.pg.org;

import cn.geoair.comp.db.service.core.typehander.BaseTypeHandler;
import cn.geoair.comp.db.service.core.typehander.TypeHandlerRegistry;
import cn.hutool.core.lang.Singleton;
import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： 这个跟PgGeom都是postgis的对象,但是两个人的共同父类不一样
 */
public class OrgPostGisGeomTypeHandler extends BaseTypeHandler<String> {

	public static void register() {
		TypeHandlerRegistry.register(org.postgis.Geometry.class, Singleton.get(OrgPostGisGeomTypeHandler.class));
	}

	@Override
	public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
		return null;
	}

	@Override
	public String getResult(Entity entity, String columnName) {
		Object obj = entity.getObj(columnName);
		if (obj instanceof org.postgis.Geometry) {
			return toWkt((org.postgis.Geometry) obj);
		}
		return String.valueOf(obj);
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
		if (obj instanceof org.postgis.Geometry) {
			return toWkt((org.postgis.Geometry) obj);
		}
		return String.valueOf(obj);
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
		if (obj instanceof org.postgis.Geometry) {
			return toWkt((org.postgis.Geometry) obj);
		}
		return String.valueOf(obj);
	}

	@Override
	public String getResult(Map<String, Object> row, String columnName) {
		Object obj = null;
		obj = row.get(columnName);
		if (obj instanceof org.postgis.Geometry) {
			return toWkt((org.postgis.Geometry) obj);
		}
		return String.valueOf(obj);
	}

	@Override
	public String getResult(Object obj) {
		if (obj instanceof org.postgis.Geometry) {
			return toWkt((org.postgis.Geometry) obj);
		}
		return String.valueOf(obj);
	}

	String toWkt(org.postgis.Geometry value) {
		String wkt;
		try {
			wkt = ((org.postgis.Geometry) value).getTypeString() + ((org.postgis.Geometry) value).getValue();
		}
		catch (Exception e) {
			wkt = value.toString();
		}
		return wkt;
	}

}
