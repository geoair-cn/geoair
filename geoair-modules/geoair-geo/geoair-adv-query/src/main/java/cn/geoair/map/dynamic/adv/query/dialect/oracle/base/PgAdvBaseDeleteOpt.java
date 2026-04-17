package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;

/** PostgreSQL删除操作实现类 */
public class PgAdvBaseDeleteOpt extends AbstractExecAdvBaseDeleteOpt {

    public PgAdvBaseDeleteOpt() {
        // 绑定MySQL专属的表名处理器
        this.dialectTableNameProcessor = OracleDialectTableNameUtil.getInstance();
    }

    // PG专属常量
    private static final int PG_MAX_IN_PARAMS = 1000;

    // ========== 实现差异化抽象方法 ==========
    @Override
    protected int getMaxInParams() {
        return PG_MAX_IN_PARAMS;
    }
}
