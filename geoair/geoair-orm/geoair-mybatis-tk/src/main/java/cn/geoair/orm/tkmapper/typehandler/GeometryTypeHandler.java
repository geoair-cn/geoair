package cn.geoair.orm.tkmapper.typehandler;

import net.postgis.jdbc.PGgeometry;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;



import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author ：张俊
 * @date ：Created in 2023/5/23 18:05 @description： 自定义类型处理器处理 Geometry 类型
 */

@MappedTypes(Geometry.class)
public class GeometryTypeHandler extends BaseTypeHandler<Geometry> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType)
			throws SQLException {
		PGgeometry pGobject = new PGgeometry();
		pGobject.setValue(parameter.toText());
		pGobject.setType("geometry");
		pGobject.getGeometry().setSrid(4326);
		ps.setObject(i, pGobject);

	}

	@Override
	public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String geom = rs.getString(columnName);
		try {
			return geom == null ? null : toGeometry(geom);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String geom = rs.getString(columnIndex);
		try {
			return geom == null ? null : toGeometry(geom);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String geom = cs.getString(columnIndex);
		try {
			return geom == null ? null : toGeometry(geom);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	private Geometry toGeometry(String geomStr) throws Exception {
		PGgeometry pgGeometry = new PGgeometry(geomStr);
		net.postgis.jdbc.geometry.Geometry geometry = pgGeometry.getGeometry();
		WKTReader wktReader = new WKTReader();
		wktReader.setIsOldJtsCoordinateSyntaxAllowed(false);
		Geometry jtsGeom = wktReader.read(geometry.getTypeString() + geometry.getValue());
		jtsGeom.setSRID(geometry.getSrid());
		return jtsGeom;
	}

}
