package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.function.Supplier;

/** PostgreSQL删除操作实现类 */
public class OracleAdvBaseDeleteOpt extends AbstractExecAdvBaseDeleteOpt {

    public OracleAdvBaseDeleteOpt(
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
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

    /**
     * Oracle分批条件删除SQL（使用ROWNUM，不支持LIMIT）
     *
     * <p>Oracle删除语法：DELETE FROM table WHERE condition AND ROWNUM <= ?
     */
    @Override
    protected String buildDeleteBatchByConditionSql(
            String tableName, String whereClause, int batchSize) {
        // Oracle使用 ROWNUM 实现分批删除
        return StrUtil.format("DELETE FROM {} WHERE {} AND ROWNUM <= ?", tableName, whereClause);
    }
}
