package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

import cn.geoair.map.dynamic.adv.query.typehandler.SqlPlaceholder;
import cn.geoair.map.dynamic.adv.query.typehandler.impl.SqlServerGeometryAdvTypeHandler;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import org.junit.Test;
import org.locationtech.jts.geom.GeometryFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** SQL Server 原生 SQL 片段和 Geometry 参数处理测试。 */
public class SqlServerDialectSupportTest {

    @Test
    public void shouldBuildBracketQuotedSqlServerPagination() {
        SqlServerDialectTableNameUtil dialect = SqlServerDialectTableNameUtil.getInstance();

        assertEquals("[order]]detail]", dialect.tbQuoteFieldName("order]detail"));
        assertEquals(
                "SELECT * FROM [feature] ORDER BY (SELECT 0) OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY",
                dialect.tbBuildPageSql("SELECT * FROM [feature]", 10, 20));
        assertEquals("dbo", dialect.tbExtractSchemaName("[dbo].[feature]"));
        assertEquals("feature", dialect.tbGetTableNameNotSchema("[dbo].[feature]"));
        assertEquals("feature]name", dialect.tbGetTableNameNotSchema("[dbo].[feature]]name]"));
    }

    @Test
    public void shouldUseGeometryConstructorAndNativeSpatialPredicates() {
        GeometryFactory factory = new GeometryFactory();
        org.locationtech.jts.geom.Point point = factory.createPoint(new org.locationtech.jts.geom.Coordinate(120, 30));
        point.setSRID(4326);

        SqlPlaceholder placeholder = new SqlServerGeometryAdvTypeHandler().getSqlPlaceholder(point);
        assertEquals("geometry::STGeomFromText(N'POINT (120 30)', 4326)", placeholder.getSql());
        assertTrue(SqlServerSpatialSql.intersects("geom", "POINT (120 30)", 4326)
                .contains("[geom].STIntersects(geometry::STGeomFromText"));
        assertEquals("[geom].STIsValid() = 0", SqlServerSpatialSql.isValid("geom"));
    }

    @Test
    public void shouldBuildNativeSqlServerDdlAndSpatialIndexSql() {
        SqlServerAdvDDLOpt ddl = new SqlServerAdvDDLOpt(null, null);

        assertEquals("DROP TABLE IF EXISTS [dbo].[feature]", ddl.buildDropTableSql("[dbo].[feature]"));
        assertEquals("SELECT TOP 0 * FROM (SELECT * FROM [feature]) AS temp_table",
                ddl.buildMetadataQuerySql("SELECT * FROM [feature]"));
        assertEquals("CREATE UNIQUE INDEX [idx_feature_name] ON [dbo].[feature] ([name])",
                ddl.buildCreateIndexSql("[dbo].[feature]", "idx_feature_name", "[name]", true));
        assertEquals("INSERT INTO [dbo].[feature_copy] SELECT * FROM [dbo].[feature]",
                ddl.buildCreateTableFromTableSql("[dbo].[feature_copy]", "[dbo].[feature]"));
    }

    @Test
    public void shouldAlterBracketQuotedColumnWithoutAccidentalRenameAndKeepMaxLength() {
        SqlServerAdvDDLOpt ddl = new SqlServerAdvDDLOpt(null, null);
        FieldBySchemaApo field = new FieldBySchemaApo();
        field.setColumnName("name");
        field.setUdtName("varchar");
        field.setCharacterMaximumLength(-1);
        field.setIsNullable("YES");

        assertEquals("ALTER TABLE [dbo].[feature] ALTER COLUMN [name] varchar(MAX) NULL",
                ddl.buildAlterColumnSql("[dbo].[feature]", "[name]", field));
    }
}
