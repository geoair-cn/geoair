package cn.geoair.comp.message.converter.jts.mybatis.impl;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.postgis.jdbc.PGgeometry;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

/**
 * @author ：张俊
 * @date ：Created in 2023/5/23 18:05 新版本的postgis驱动
 */

// @MappedTypes(Geometry.class)
// @MappedJdbcTypes(JdbcType.OTHER)
public class NetPgGeometryTypeHandler /*extends BaseTypeHandler<Geometry> */ {

    static NetPgGeometryTypeHandler netPgGeometryTypeHandler;

    public static void register(TypeHandlerRegistry typeHandlerRegistry) {
        typeHandlerRegistry.register(PGgeometry.class, NetPgGeometryTypeHandler.class);
    }

    public static NetPgGeometryTypeHandler getInstance() {
        if (netPgGeometryTypeHandler == null) {
            netPgGeometryTypeHandler = new NetPgGeometryTypeHandler();
        }
        return netPgGeometryTypeHandler;
    }


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


    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String geom = rs.getString(columnName);
        try {
            return geom == null ? null : toGeometry(geom);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String geom = rs.getString(columnIndex);
        try {
            return geom == null ? null : toGeometry(geom);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


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
        net.postgis.jdbc.geometry.Geometry geometry = pgGeometry.getGeometry();
        WKTReader wktReader = new WKTReader();
        wktReader.setIsOldJtsCoordinateSyntaxAllowed(false);
        Geometry jtsGeom = wktReader.read(geometry.getTypeString() + geometry.getValue());
        jtsGeom.setSRID(geometry.getSrid());
        return jtsGeom;
    }
}
