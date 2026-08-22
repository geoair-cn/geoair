package cn.geoair.comp.jdbc.url;

import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import cn.hutool.db.dialect.DialectName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 默认 JDBC URL 编解码器的主要语法覆盖测试。
 *
 * @author 张逢吉
 */
class DefaultJdbcUrlCodecTest {
    private final JdbcUrlCodec codec = GirJdbcUrlCodecs.defaultCodec();

    @Test
    void shouldParseAndRewritePostgreSqlUrl() {
        String original = "jdbc:postgresql://db1:5432,db2:5433/gis?currentSchema=old&sslmode=require";

        JdbcUrl jdbcUrl = codec.parse(original);

        Assertions.assertEquals(DatabaseType.POSTGRESQL, jdbcUrl.getDatabaseType());
        Assertions.assertEquals("db1", jdbcUrl.getPrimaryEndpoint().getHost());
        Assertions.assertEquals(Integer.valueOf(5432), jdbcUrl.getPrimaryEndpoint().getPort());
        Assertions.assertEquals("gis", jdbcUrl.getDatabaseName());
        Assertions.assertEquals("old", codec.getSchema(original));
        Assertions.assertEquals("jdbc:postgresql://db1:5432,db2:5433/gis?currentSchema=geo+data&sslmode=require",
                codec.rewriteSchema(original, "geo data"));
    }

    @Test
    void shouldKeepOracleSidAndServiceNameForms() {
        JdbcUrl sid = codec.parse("jdbc:oracle:thin:@db1:1521:ORCL?defaultSchema=GIS");
        JdbcUrl serviceName = codec.parse("jdbc:oracle:thin:@//db2:1522/ORCLPDB1");

        Assertions.assertEquals("thin", sid.getSubProtocol());
        Assertions.assertEquals("db1", sid.getPrimaryEndpoint().getHost());
        Assertions.assertEquals("ORCL", sid.getDatabaseName());
        Assertions.assertEquals("db2", serviceName.getPrimaryEndpoint().getHost());
        Assertions.assertEquals("ORCLPDB1", serviceName.getDatabaseName());
        Assertions.assertEquals("jdbc:oracle:thin:@db1:1521:ORCL?defaultSchema=NEW",
                codec.rewriteSchema("jdbc:oracle:thin:@db1:1521:ORCL?defaultSchema=GIS", "NEW"));
    }

    @Test
    void shouldUseSemicolonPropertiesForSqlServerAndH2() {
        String sqlServer = "jdbc:sqlserver://sql:1433;databaseName=gis;encrypt=true";
        String h2 = "jdbc:h2:./data/gis;MODE=PostgreSQL;schema=PUBLIC";

        Assertions.assertEquals("gis", codec.parse(sqlServer).getDatabaseName());
        Assertions.assertEquals("jdbc:sqlserver://sql:1433;databaseName=gis;encrypt=true;schemaName=gis_schema",
                codec.rewriteSchema(sqlServer, "gis_schema"));
        Assertions.assertEquals("jdbc:h2:./data/gis;MODE=PostgreSQL;schema=GIS",
                codec.rewriteSchema(h2, "GIS"));
    }

    @Test
    void shouldBuildLegacySupportedUrls() {
        Assertions.assertEquals("jdbc:mysql://mysql:3306/gis", codec.format(
                codec.create(DatabaseType.MYSQL, "mysql", 3306, "gis")));
        Assertions.assertEquals("jdbc:sap://hana:30015/?databaseName=SYSTEMDB", codec.format(
                codec.create(DatabaseType.SAP_HANA, "hana", 30015, "SYSTEMDB")));
        Assertions.assertEquals("jdbc:phoenix:zookeeper:2181", codec.format(
                codec.create(DatabaseType.PHOENIX, "zookeeper", 2181, null)));
    }

    @Test
    void shouldUseHutoolDialectAndDriverMapping() {
        Assertions.assertEquals(DialectName.POSTGRESQL, DatabaseType.POSTGRESQL.getDialectName());
        Assertions.assertEquals(DatabaseType.MYSQL,
                DatabaseType.fromDriverClassName(DatabaseType.MYSQL.getDriverClassName()));
        Assertions.assertEquals(DatabaseType.SQLSERVER,
                DatabaseType.fromDialectName(DialectName.SQLSERVER));
    }

    @Test
    void shouldRejectSchemaRewriteForDatabaseWithoutSchemaProperty() {
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> codec.rewriteSchema("jdbc:sqlite:/tmp/gis.db", "main"));
    }
}
