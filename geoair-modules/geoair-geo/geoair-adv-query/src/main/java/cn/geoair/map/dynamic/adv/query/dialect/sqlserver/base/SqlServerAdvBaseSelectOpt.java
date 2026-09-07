package cn.geoair.map.dynamic.adv.query.dialect.sqlserver.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.SqlServerDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;

import java.util.function.Supplier;

/**
 * SQL Server 基础查询实现。@author 张逢吉
 */
public class SqlServerAdvBaseSelectOpt extends AbstractExecAdvBaseSelectOpt {
    public SqlServerAdvBaseSelectOpt(
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = SqlServerDialectTableNameUtil.getInstance();
    }
}
