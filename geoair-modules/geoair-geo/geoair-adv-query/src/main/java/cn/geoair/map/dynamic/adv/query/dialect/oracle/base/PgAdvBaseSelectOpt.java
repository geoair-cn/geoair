package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;

/** PostgreSQL查询操作实现类 */
public class PgAdvBaseSelectOpt extends AbstractExecAdvBaseSelectOpt {

    // 初始化表名处理器
    public PgAdvBaseSelectOpt() {
        this.dialectTableNameProcessor = OracleDialectTableNameUtil.getInstance();
    }
}
