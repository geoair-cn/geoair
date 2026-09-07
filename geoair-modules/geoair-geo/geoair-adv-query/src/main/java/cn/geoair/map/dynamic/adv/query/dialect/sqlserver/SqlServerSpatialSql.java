package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

import cn.hutool.core.util.StrUtil;

/**
 * SQL Server {@code geometry} 空间 SQL 片段构建器。
 * <p>本类只生成不依赖表元数据的原生空间表达式，供后续 SQL Server 执行器复用。</p>
 *
 * @author 张逢吉
 */
public final class SqlServerSpatialSql {

    private SqlServerSpatialSql() {
    }

    public static String geometryFromText(String wkt, int srid) {
        if (StrUtil.isBlank(wkt)) {
            throw new IllegalArgumentException("WKT 不能为空");
        }
        return "geometry::STGeomFromText(N'" + wkt.replace("'", "''") + "', " + srid + ")";
    }

    public static String intersects(String geometryColumn, String wkt, int srid) {
        return quoteField(geometryColumn) + ".STIntersects(" + geometryFromText(wkt, srid) + ") = 1";
    }

    public static String within(String geometryColumn, String bboxWkt, int srid) {
        return quoteField(geometryColumn) + ".STWithin(" + geometryFromText(bboxWkt, srid) + ") = 1";
    }

    public static String distance(String geometryColumn, String wkt, int srid) {
        return quoteField(geometryColumn) + ".STDistance(" + geometryFromText(wkt, srid) + ")";
    }

    public static String centroid(String geometryColumn) {
        return quoteField(geometryColumn) + ".STCentroid()";
    }

    public static String isValid(String geometryColumn) {
        return quoteField(geometryColumn) + ".STIsValid() = 0";
    }

    public static String makeValid(String geometryColumn) {
        return quoteField(geometryColumn) + ".MakeValid()";
    }

    private static String quoteField(String fieldName) {
        return SqlServerDialectTableNameUtil.getInstance().tbQuoteFieldName(fieldName);
    }
}
