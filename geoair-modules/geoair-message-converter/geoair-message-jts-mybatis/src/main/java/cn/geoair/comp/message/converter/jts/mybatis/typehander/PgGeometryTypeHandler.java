package cn.geoair.comp.message.converter.jts.mybatis.typehander;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;

import cn.geoair.map.dynamic.tools.convert.GirPostGisTran;

/**
 * @author ：张逢吉
 * @date ：Created in 16:53 @description： TODO
 */
@MappedTypes(Geometry.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgGeometryTypeHandler extends BaseTypeHandler<Geometry> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType)
			throws SQLException {
		if (GirPostGisTran.isNetConvert()) {
			NetPgGeometryTypeHandler.getInstance().setNonNullParameter(ps, i, parameter, jdbcType);
			return;
		}
		if (GirPostGisTran.isOrgConvert()) {
			NetPgGeometryTypeHandler.getInstance().setNonNullParameter(ps, i, parameter, jdbcType);
		}
	}

	@Override
	public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
		if (GirPostGisTran.isNetConvert()) {
			return NetPgGeometryTypeHandler.getInstance().getNullableResult(rs, columnName);

		}
		if (GirPostGisTran.isOrgConvert()) {
			return NetPgGeometryTypeHandler.getInstance().getNullableResult(rs, columnName);
		}
		return null;
	}

	@Override
	public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		if (GirPostGisTran.isNetConvert()) {
			return NetPgGeometryTypeHandler.getInstance().getNullableResult(rs, columnIndex);
		}
		if (GirPostGisTran.isOrgConvert()) {
			return NetPgGeometryTypeHandler.getInstance().getNullableResult(rs, columnIndex);
		}
		return null;
	}

	@Override
	public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		if (GirPostGisTran.isNetConvert()) {
			return NetPgGeometryTypeHandler.getInstance().getNullableResult(cs, columnIndex);
		}
		if (GirPostGisTran.isOrgConvert()) {
			return NetPgGeometryTypeHandler.getInstance().getNullableResult(cs, columnIndex);
		}
		return null;
	}

}
