package cn.geoair.map.dynamic.adv.query.dialect.sqlite.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.SqliteDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;

import java.util.function.Supplier;

/** SQLite 查询操作。 */
public class SqliteAdvBaseSelectOpt extends AbstractExecAdvBaseSelectOpt {

    public SqliteAdvBaseSelectOpt(
            Supplier<AdvQueryGlobalConfig> configGetter, AdvTypeHandlerRegistry registry) {
        super(configGetter, registry);
        this.dialectTableNameProcessor = SqliteDialectTableNameUtil.getInstance();
    }
}
