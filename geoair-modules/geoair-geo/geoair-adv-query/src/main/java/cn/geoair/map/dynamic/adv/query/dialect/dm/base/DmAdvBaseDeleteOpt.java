package cn.geoair.map.dynamic.adv.query.dialect.dm.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.DmDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;

import java.util.function.Supplier;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦删除操作实现类
 */
public class DmAdvBaseDeleteOpt extends AbstractExecAdvBaseDeleteOpt {

    private static final int DM_MAX_IN_PARAMS = 1000;

    public DmAdvBaseDeleteOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        this.dialectTableNameProcessor = DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected int getMaxInParams() {
        return DM_MAX_IN_PARAMS;
    }

    @Override
    protected String buildDeleteBatchByConditionSql(
            String tableName, String whereClause, int batchSize) {
        return StrUtil.format("DELETE FROM {} WHERE {} AND ROWNUM <= ?", tableName, whereClause);
    }
}
