package cn.geoair.map.dynamic.adv.query.dialect.sqlite;

import cn.geoair.map.dynamic.adv.dbmeta.SqliteType;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** SQLite Core 的真实 JDBC 集成测试。 */
public class SqliteDialectIntegrationTest {

    private Path databaseFile;
    private IAdvExecutor executor;

    @Before
    public void setUp() throws Exception {
        databaseFile = Files.createTempFile("geoair-adv-query-", ".sqlite");
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        executor = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, "sqlite-test");
        executor.dExecuteDDL(
                "CREATE TABLE people ("
                        + "id INTEGER PRIMARY KEY, "
                        + "name VARCHAR(40) NOT NULL, "
                        + "score NUMERIC(10,2) DEFAULT 0)",
                "people",
                "创建测试表");
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(databaseFile);
    }

    @Test
    public void shouldExecuteCrudPaginationUpsertAndMetadata() {
        assertEquals(1, executor.bInsertOne("people", row(1, "Alice", 91.5)).intValue());
        assertEquals(1, executor.bInsertOne("people", row(2, "Bob", 82)).intValue());
        assertEquals(1, executor.bInsertOne("people", row(3, "Carol", 88)).intValue());

        assertEquals(
                0,
                executor.bInsertIgnore(
                        "people", row(1, "ignored", 0), Arrays.asList("id"))
                        .intValue());
        assertEquals(
                1,
                executor.bUpsert(
                        "people", row(2, "Bobby", 95), Arrays.asList("id"))
                        .intValue());

        GirAdvOneRow bob = executor.bSelectOne("SELECT * FROM people WHERE id = 2");
        assertNotNull(bob);
        assertEquals("Bobby", bob.getStr("name"));

        PageApo<GirAdvOneRow> page = executor.pPage(
                "SELECT * FROM people ORDER BY id", 1, 2);
        assertEquals(3L, page.getTotal());
        assertEquals(2, page.geRecordsList().size());
        assertEquals(1, page.geRecordsList().get(0).getInt("id").intValue());

        DataFieldsApo fields = executor.dGetColumnsByTable("people");
        assertEquals(Arrays.asList("id"), executor.dGetPrimaryKeys("people"));
        assertEquals(3, fields.getDataFieldList().size());
        assertFalse(fields.getDataFieldList().get(0).isGeometryFieldIs());
        FieldBySchemaApo nameField = fields.findField(f -> "name".equals(f.getColumnName()))
                .orElseThrow(IllegalStateException::new);
        FieldBySchemaApo scoreField = fields.findField(f -> "score".equals(f.getColumnName()))
                .orElseThrow(IllegalStateException::new);
        assertEquals(SqliteType.TEXT, nameField.getDbType());
        assertEquals(SqliteType.NUMERIC, scoreField.getDbType());
        assertEquals(Integer.valueOf(40), nameField.getCharacterMaximumLength());
        assertEquals(Integer.valueOf(10), scoreField.getNumericPrecision());
        assertEquals(Integer.valueOf(2), scoreField.getNumericScale());
        assertEquals("main", executor.dGetCurrentSchema());
        assertTrue(executor.dGetCurrentDataBase().endsWith(".sqlite"));
        assertTrue(executor.dGetAllSchemas().contains("main"));
        assertTrue(executor.dGetTablesBySchema().contains("people"));
        assertTrue(executor.dIsFunctionExists("lower"));

        executor.dCreateIndex("people", "idx_people_name", Arrays.asList("name"), false);
        assertTrue(executor.dIndexesExists("people", "idx_people_name"));
        executor.dDropIndex("people", "idx_people_name");
        assertFalse(executor.dIndexesExists("people", "idx_people_name"));

        assertEquals(
                1,
                executor.bUpdateByPK(
                        "people", "id", 3, singletonRow("name", "Caroline"))
                        .intValue());
        assertEquals(1, executor.bDeleteByPK("people", "id", 1).intValue());
        assertEquals(2L, executor.bSelectRecordRowCount("SELECT * FROM people").longValue());
    }

    @Test
    public void shouldExecuteSupportedTableDdl() {
        executor.bInsertOne("people", row(1, "Alice", 91.5));
        executor.bInsertOne("people", row(2, "Bob", 82));

        executor.dCopyTableByTableName("people_copy", "people", true);
        assertTrue(executor.dIsTableExists("people_copy"));
        assertEquals(
                2L,
                executor.bSelectRecordRowCount("SELECT * FROM people_copy").longValue());

        executor.dDropColumn("people_copy", "score");
        assertEquals(
                Arrays.asList("id", "name"),
                executor.dGetColumnsByTable("people_copy").inOrdinalOrder().fieldNames());

        executor.dRenameTable("people_copy", "people_archive");
        assertFalse(executor.dIsTableExists("people_copy"));
        assertTrue(executor.dIsTableExists("people_archive"));

        executor.dTruncateTable("people");
        assertEquals(0L, executor.bSelectRecordRowCount("SELECT * FROM people").longValue());

        executor.dDropTable("people_archive");
        assertFalse(executor.dIsTableExists("people_archive"));
    }

    @Test
    public void shouldRollbackTransaction() {
        try {
            executor.tx(() -> {
                executor.bInsertOne("people", row(99, "rollback", 1));
                throw new IllegalStateException("force rollback");
            });
            fail("事务异常应向外抛出");
        } catch (IllegalStateException expected) {
            assertEquals("force rollback", expected.getMessage());
        }

        assertTrue(executor.bSelectOne("SELECT * FROM people WHERE id = 99").isEmpty());
    }

    @Test
    public void shouldRejectSpatialAndUnsupportedSchemaOperations() {
        assertUnsupported(() -> executor.eGetAllGeoLayerName(), "SpatiaLite");
        assertUnsupported(() -> executor.dCreateSchema("other"), "CREATE/DROP SCHEMA");
        assertUnsupported(
                () -> executor.dAddPrimaryKey("people", Arrays.asList("name"), "pk_people"),
                "事后增删主键");
        assertNull(executor.dGetTableSize("people"));
    }

    @Test
    public void shouldAdaptOnlyWellDefinedConditionOperators() {
        executor.bInsertOne("people", row(1, "Alice", 91.5));
        executor.bInsertOne("people", row(2, "Bob", 82));

        GirAdvQueryRequest filteredQuery = GirAdvQueryRequest.builder()
                .table("people")
                .fields("id", "name")
                .where(GirAdvWhereFilter.of()
                        .in("id", Arrays.asList(1, 2))
                        .ge("score", 90))
                .build();
        List<GirAdvOneRow> filteredRows = executor.wSelectList(filteredQuery);
        assertEquals(1, filteredRows.size());
        assertEquals("Alice", filteredRows.get(0).getStr("name"));

        GirAdvQueryRequest nullSafeQuery = GirAdvQueryRequest.builder()
                .table("people")
                .fields("id")
                .where(GirAdvWhereFilter.of()
                        .addCondition("id", AdvOperatorEnums.EQUAL_NULL_SAFE, 2))
                .build();
        List<GirAdvOneRow> nullSafeRows = executor.wSelectList(nullSafeQuery);
        assertEquals(1, nullSafeRows.size());
        assertEquals(2, nullSafeRows.get(0).getInt("id").intValue());

        assertEquals(
                "IS",
                SqliteDialectTableNameUtil.getInstance()
                        .tbGetOperatorSql(AdvOperatorEnums.EQUAL_NULL_SAFE));
        assertUnsupported(
                () -> executor.wSelectList(GirAdvQueryRequest.builder()
                        .table("people")
                        .fields("id")
                        .where(GirAdvWhereFilter.of()
                                .addCondition("name", AdvOperatorEnums.ILIKE_ALL, "ali"))
                        .build()),
                "ilike");
    }

    private static Map<String, Object> row(int id, String name, Number score) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("score", score);
        return row;
    }

    private static Map<String, Object> singletonRow(String key, Object value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(key, value);
        return row;
    }

    private static void assertUnsupported(Runnable runnable, String messagePart) {
        try {
            runnable.run();
            fail("应抛出 UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains(messagePart));
        }
    }
}
