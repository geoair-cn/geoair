package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.comp.dynamic.ds.MockDataSourceGetter;
import cn.geoair.comp.dynamic.ds.base.IDsDataSourceOpt;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/17 18:20
 * @description： 快速获取创建器，但是这只是一个简单地测试用的示例 基于不同的数据库类型
 */
public class GirAdvSqlComposerMockProvider {

    public static GirAdvSqlComposer getMockMysql() {
        DialectTableNameProcessor masql = MysqlDialectTableNameUtil.getInstance();
        IDsDataSourceOpt dataSourceOpt = MockDataSourceGetter.getInstance();
        return new GirAdvSqlComposer(masql, dataSourceOpt);
    }

    public static GirAdvSqlComposer getMockPostgresql() {
        DialectTableNameProcessor masql = PgDialectTableNameUtil.getInstance();
        IDsDataSourceOpt dataSourceOpt = MockDataSourceGetter.getInstance();
        return new GirAdvSqlComposer(masql, dataSourceOpt);
    }

    public static GirAdvSqlComposer getMockOracle() {
        DialectTableNameProcessor masql = OracleDialectTableNameUtil.getInstance();
        IDsDataSourceOpt dataSourceOpt = MockDataSourceGetter.getInstance();
        return new GirAdvSqlComposer(masql, dataSourceOpt);
    }
}
