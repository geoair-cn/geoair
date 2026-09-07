package cn.geoair.map.dynamic.file.jdbc;

import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoLinkInfo;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/** JDBC URL 到 GeoTools 连接参数的无数据库契约测试。 */
public class JdbcGeoDataStoreParamsTest {

    @Test
    public void shouldCreateMysqlDataStoreParamsFromJdbcUrl() {
        Map<String, Object> params = JdbcGeoDataStoreParams.create("mysql", linkInfo(
                "jdbc:mysql://10.0.0.8:3307/source_db?useSSL=false", "source_schema"));

        assertEquals("mysql", params.get("dbtype"));
        assertEquals("10.0.0.8", params.get("host"));
        assertEquals(3307, params.get("port"));
        assertEquals("source_db", params.get("database"));
        assertEquals("source_schema", params.get("schema"));
    }

    @Test
    public void shouldCreateOracleDataStoreParamsFromServiceNameUrl() {
        Map<String, Object> params = JdbcGeoDataStoreParams.create("oracle", linkInfo(
                "jdbc:oracle:thin:@//10.0.0.9:1521/ORCLPDB1", "GIS"));

        assertEquals("oracle", params.get("dbtype"));
        assertEquals("10.0.0.9", params.get("host"));
        assertEquals(1521, params.get("port"));
        assertEquals("ORCLPDB1", params.get("database"));
        assertEquals("GIS", params.get("schema"));
    }

    private JdbcGeoLinkInfo linkInfo(String jdbcUrl, String schema) {
        return new TestJdbcGeoLinkInfo()
                .setJdbcUrl(jdbcUrl)
                .setUsername("geoair")
                .setPassword("secret")
                .setSchema(schema);
    }

    private static class TestJdbcGeoLinkInfo extends JdbcGeoLinkInfo {
        @Override
        protected String getDriverClassName() {
            return "java.lang.String";
        }

        @Override
        public void checkLinkInfo() {
            // URL 参数测试不访问真实数据库。
        }
    }
}
