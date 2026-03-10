package cn.geoair.map.dynamic.dbservice.typehander.oracle;

import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import cn.geoair.map.dynamic.dbservice.typehander.BaseTypeHandler;

import oracle.spatial.util.ByteOrder;
import oracle.spatial.util.WKB;
import oracle.sql.STRUCT;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： oracle空间字段的解析
 */
public class OracleGeomTypeHandler extends BaseTypeHandler<String> {

	@Override
	public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
		return null;
	}

	@Override
	public String getResult(Entity entity, String columnName) {
		Object obj = entity.getObj(columnName);
		if (obj instanceof STRUCT) {
			return toWkt((STRUCT) obj);
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
		if (obj instanceof STRUCT) {
			return toWkt((STRUCT) obj);
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
		if (obj instanceof STRUCT) {
			return toWkt((STRUCT) obj);
		}
		return String.valueOf(obj);
	}

	@Override
	public String getResult(Map<String, Object> row, String columnName) {
		Object obj = null;
		obj = row.get(columnName);
		if (obj instanceof STRUCT) {
			return toWkt((STRUCT) obj);
		}
		return String.valueOf(obj);
	}

	@Override
	public String getResult(Object obj) {
		if (obj instanceof STRUCT) {
			return toWkt((STRUCT) obj);
		}
		return String.valueOf(obj);
	}

	String toWkt(STRUCT value) {
		String wkt;
		try {
			WKB wkb = new WKB(ByteOrder.BIG_ENDIAN);
			byte[] b = wkb.fromSTRUCT((STRUCT) value); // convert: Object -> STRUCT ->
														// byte[]
			Geometry jtsGeom = (new WKBReader()).read(b); // convert: byte[] ->
															// JTS-Geometry
			wkt = jtsGeom.toString();
		}
		catch (Exception e) {
			return "无法解析空间数据";
		}
		return wkt;
	}

}
