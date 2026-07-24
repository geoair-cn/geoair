package cn.geoair.comp.message.converter.jts.mybatis.test;

import cn.geoair.comp.message.converter.jts.mybatis.typehander.PgGeometryTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

/** PgGeometryTypeHandler 示例 */
public class PgGeometryTypeHandlerExample {

    public static void main(String[] args) throws Exception {
        PgGeometryTypeHandler handler = new PgGeometryTypeHandler();
        Geometry geometry = new WKTReader().read("POINT (116.40 39.90)");

        System.out.println("handler = " + handler.getClass().getSimpleName());
        System.out.println("geometry = " + geometry);
        System.out.println("jdbcType = " + JdbcType.OTHER);
    }
}
