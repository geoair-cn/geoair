package cn.geoair.comp.message.converter.jts.mybatis.test;

import cn.geoair.comp.message.converter.jts.mybatis.typehander.PgGeometryTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Geometry TypeHandler 示例
 */
public class PgGeometryTypeHandlerExample {

    public static void main(String[] args) {
        GeometryFactory factory = new GeometryFactory();
        Geometry point = factory.createPoint(new Coordinate(116.40, 39.90));
        PgGeometryTypeHandler handler = new PgGeometryTypeHandler();

        System.out.println("handler = " + handler.getClass().getSimpleName());
        System.out.println("mapped geometry = " + point);
        System.out.println("jdbcType = " + JdbcType.OTHER);
    }
}
