package cn.geoair.map.dynamic.adv.query.dialect.sqlserver.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.SqlServerDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;

import java.util.function.Supplier;

/**
 * SQL Server 删除操作实现。@author 张逢吉
 */
public class SqlServerAdvBaseDeleteOpt extends AbstractExecAdvBaseDeleteOpt {
    private static final int SQL_SERVER_MAX_IN_PARAMS = 2100;

    public SqlServerAdvBaseDeleteOpt(
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = SqlServerDialectTableNameUtil.getInstance();
    }

    @Override
    protected int getMaxInParams() {
        return SQL_SERVER_MAX_IN_PARAMS;
    }
}
