package cn.geoair.comp.message.converter.jts.mybatis.impl;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.postgis.PGgeometry;

/**
 * @author ：张俊
 * @date ：旧版本的postgis驱动
 */

// @MappedTypes(Geometry.class)
// @MappedJdbcTypes(JdbcType.OTHER)
public class OrgPgGeometryTypeHandler extends BaseTypeHandler<Geometry> {

    public static void register(TypeHandlerRegistry typeHandlerRegistry) {
        typeHandlerRegistry.register(PGgeometry.class, OrgPgGeometryTypeHandler.class);
    }

    static OrgPgGeometryTypeHandler pgGeometryTypeHandler;

    public static OrgPgGeometryTypeHandler getInstance() {
        if (pgGeometryTypeHandler == null) {
            pgGeometryTypeHandler = new OrgPgGeometryTypeHandler();
        }
        return pgGeometryTypeHandler;
    }

    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType)
            throws SQLException {
        PGgeometry pGobject = new PGgeometry();
        pGobject.setValue(parameter.toText());
        pGobject.setType("geometry");
        int srid = parameter.getSRID();
        if (srid != 0) {
            pGobject.getGeometry().setSrid(srid);
        }
        ps.setObject(i, pGobject);
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String geom = rs.getString(columnName);
        try {
            return geom == null ? null : toGeometry(geom);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String geom = rs.getString(columnIndex);
        try {
            return geom == null ? null : toGeometry(geom);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String geom = cs.getString(columnIndex);
        try {
            return geom == null ? null : toGeometry(geom);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Geometry toGeometry(String geomStr) throws Exception {
        PGgeometry pgGeometry = new PGgeometry(geomStr);
        org.postgis.Geometry geometry = pgGeometry.getGeometry();
        WKTReader wktReader = new WKTReader();
        wktReader.setIsOldJtsCoordinateSyntaxAllowed(false);
        Geometry jtsGeom = wktReader.read(geometry.getTypeString() + geometry.getValue());
        jtsGeom.setSRID(geometry.getSrid());
        return jtsGeom;
    }
}
