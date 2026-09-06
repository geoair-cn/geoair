package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgAdvDDLOpt;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** 表复制的方言 SQL 回归测试。 */
public class DdlCopySqlTest {

    @Test
    public void shouldCopyDataIntoAlreadyCreatedTableForEveryDialect() {
        assertEquals("INSERT INTO `target` SELECT * FROM `source`",
                new TestMysqlDdl().copyDataSql("`target`", "`source`"));
        assertEquals("INSERT INTO \"target\" SELECT * FROM \"source\"",
                new TestPgDdl().copyDataSql("\"target\"", "\"source\""));
        assertEquals("INSERT INTO \"TARGET\" SELECT * FROM \"SOURCE\"",
                new TestOracleDdl().copyDataSql("\"TARGET\"", "\"SOURCE\""));
    }

    @Test
    public void shouldNotSilentlyReuseExistingTableWhenCreatingStructure() {
        assertEquals("CREATE TABLE `target` LIKE `source`",
                new TestMysqlDdl().copyStructureSql("`target`", "`source`"));
        assertEquals("CREATE TABLE \"target\" (LIKE \"source\" INCLUDING ALL)",
                new TestPgDdl().copyStructureSql("\"target\"", "\"source\""));
    }

    private static class TestMysqlDdl extends MysqlAdvDDLOpt {
        private TestMysqlDdl() {
            super(null, null);
        }

        private String copyDataSql(String target, String source) {
            return buildCreateTableFromTableSql(target, source);
        }

        private String copyStructureSql(String target, String source) {
            return buildCreateTableLikeSql(target, source);
        }
    }

    private static class TestPgDdl extends PgAdvDDLOpt {
        private TestPgDdl() {
            super(null, null);
        }

        private String copyDataSql(String target, String source) {
            return buildCreateTableFromTableSql(target, source);
        }

        private String copyStructureSql(String target, String source) {
            return buildCreateTableLikeSql(target, source);
        }
    }

    private static class TestOracleDdl extends OracleAdvDDLOpt {
        private TestOracleDdl() {
            super(null, null);
        }

        private String copyDataSql(String target, String source) {
            return buildCreateTableFromTableSql(target, source);
        }
    }
}
