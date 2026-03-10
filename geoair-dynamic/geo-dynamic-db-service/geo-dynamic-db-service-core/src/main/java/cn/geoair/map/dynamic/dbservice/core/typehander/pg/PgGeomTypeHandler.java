package cn.geoair.map.dynamic.dbservice.core.typehander.pg;

import cn.geoair.map.dynamic.dbservice.core.typehander.BaseTypeHandler;
import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import net.postgis.jdbc.PGgeometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： TODO
 */
public class PgGeomTypeHandler extends BaseTypeHandler<String> {

	WKTReader wktReader = new WKTReader(new GeometryFactory(new PrecisionModel(), 4326));

	@Override
	public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
		return null;
	}

	@Override
	public String getResult(Entity entity, String columnName) {
		Object obj = entity.getObj(columnName);
		if (obj instanceof PGgeometry) {
			return toWkt((PGgeometry) obj);
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
		if (obj instanceof PGgeometry) {
			return toWkt((PGgeometry) obj);
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
		if (obj instanceof PGgeometry) {
			return toWkt((PGgeometry) obj);
		}
		return String.valueOf(obj);
	}

	@Override
	public String getResult(Map<String, Object> row, String columnName) {
		Object obj = null;
		obj = row.get(columnName);
		if (obj instanceof PGgeometry) {
			return toWkt((PGgeometry) obj);
		}
		return String.valueOf(obj);
	}

	@Override
	public String getResult(Object obj) {
		if (obj instanceof PGgeometry) {
			return toWkt((PGgeometry) obj);
		}
		return String.valueOf(obj);
	}

	String toWkt(PGgeometry value) {
		String wkt;
		net.postgis.jdbc.geometry.Geometry geometry = value.getGeometry();
		wkt = geometry.getTypeString() + geometry.getValue();
		Geometry jtsGeom;
		try {
			jtsGeom = wktReader.read(wkt);
		}
		catch (ParseException e) {
			jtsGeom = null;
			return "无法解析空间数据";
		}
		return jtsGeom.toString();
	}

}
