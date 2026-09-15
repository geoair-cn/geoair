package cn.geoair.map.dynamic.adv.query.dialect.sqlite.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.SqliteDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;

import java.util.function.Supplier;

/** SQLite 删除操作。 */
public class SqliteAdvBaseDeleteOpt extends AbstractExecAdvBaseDeleteOpt {

    private static final int SQLITE_MAX_IN_PARAMS = 999;

    public SqliteAdvBaseDeleteOpt(
            Supplier<AdvQueryGlobalConfig> configGetter, AdvTypeHandlerRegistry registry) {
        super(configGetter, registry);
        this.dialectTableNameProcessor = SqliteDialectTableNameUtil.getInstance();
    }

    @Override
    protected int getMaxInParams() {
        return SQLITE_MAX_IN_PARAMS;
    }
}
