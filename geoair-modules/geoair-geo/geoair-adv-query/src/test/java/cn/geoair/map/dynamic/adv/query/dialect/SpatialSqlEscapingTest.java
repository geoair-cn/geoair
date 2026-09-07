package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlAdvGeoOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleAdvGeoOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgAdvGeoOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 空间 SQL 文本参数与别名的转义回归测试。 */
public class SpatialSqlEscapingTest {

    private static final String MALICIOUS_WKT = "POINT(1 2)' OR 1=1 --";

    @Test
    public void shouldEscapeWktLiteralForMysqlAndQuoteDistanceAlias() {
        MysqlAdvGeoOpt opt = new MysqlAdvGeoOpt(null, null, null);
        String sql = opt.getCalculateDistanceSql("geom", MALICIOUS_WKT, 4326, "distance-value", "`test`");
        assertTrue(sql.contains("POINT(1 2)'' OR 1=1 --"));
        assertTrue(sql.contains("AS `distance-value`"));
        assertFalse(sql.contains("POINT(1 2)' OR 1=1 --"));
    }

    @Test
    public void shouldEscapeWktLiteralForPostgisAndQuoteDistanceAlias() {
        PgAdvGeoOpt opt = new PgAdvGeoOpt(null, null, null);
        String sql = opt.getCalculateDistanceSql("geom", MALICIOUS_WKT, 4326, "distance-value", "\"test\"");
        assertTrue(sql.contains("POINT(1 2)'' OR 1=1 --"));
        assertTrue(sql.contains("AS \"distance-value\""));
        assertFalse(sql.contains("POINT(1 2)' OR 1=1 --"));
    }

    @Test
    public void shouldEscapeWktLiteralForOracleAndQuoteDistanceAlias() {
        OracleAdvGeoOpt opt = new OracleAdvGeoOpt(null, null, null);
        String sql = opt.getCalculateDistanceSql("geom", MALICIOUS_WKT, 4326, "distance-value", "\"test\"");
        assertTrue(sql.contains("POINT(1 2)'' OR 1=1 --"));
        assertTrue(sql.contains("AS \"distance-value\""));
        assertFalse(sql.contains("POINT(1 2)' OR 1=1 --"));
    }

    @Test
    public void shouldEscapeIdentifierQuoteAndQuoteCentroidAlias() {
        assertTrue(MysqlDialectTableNameUtil.getInstance().tbQuoteFieldName("a`b")
                .equals("`a``b`"));
        assertTrue(PgDialectTableNameUtil.getInstance().tbQuoteFieldName("a\"b")
                .equals("\"a\"\"b\""));
        assertTrue(OracleDialectTableNameUtil.getInstance().tbQuoteTableName("a\"b")
                .equals("\"a\"\"b\""));

        String pgSql = new PgAdvGeoOpt(null, null, null)
                .getCentroidSql("geom", "center\"name", "\"test\"");
        String oracleSql = new OracleAdvGeoOpt(null, null, null)
                .getCentroidSql("geom", "center\"name", "\"test\"");
        String mysqlValidationSql = new MysqlAdvGeoOpt(null, null, null)
                .getValidateGeometriesSql("`test`", "geom`name");
        assertTrue(pgSql.contains("AS \"center\"\"name\""));
        assertTrue(oracleSql.contains("AS \"center\"\"name\""));
        assertTrue(mysqlValidationSql.contains("ST_IsValid(`geom``name`)"));
    }
}
