package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.MockDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/17 18:20
 * @description： 快速获取创建器，基于不同的数据库类型
 */
public class GirAdvQuerySqlBuilderExample {

    public static GirAdvQuerySqlBuilder getGirAdvQuerySqlBuilderMysql() {
        DialectTableNameProcessor masql = MysqlDialectTableNameUtil.getInstance();
        IDataSourceGetter dataSourceGetter = MockDataSourceGetter.getInstance();
        return new GirAdvQuerySqlBuilder(masql, dataSourceGetter);
    }

    public static GirAdvQuerySqlBuilder getGirAdvQuerySqlBuilderPg() {
        DialectTableNameProcessor masql = PgDialectTableNameUtil.getInstance();
        IDataSourceGetter dataSourceGetter = MockDataSourceGetter.getInstance();
        return new GirAdvQuerySqlBuilder(masql, dataSourceGetter);
    }

    public static GirAdvQuerySqlBuilder getGirAdvQuerySqlBuilderOracle() {
        DialectTableNameProcessor masql = OracleDialectTableNameUtil.getInstance();
        IDataSourceGetter dataSourceGetter = MockDataSourceGetter.getInstance();
        return new GirAdvQuerySqlBuilder(masql, dataSourceGetter);
    }
}
