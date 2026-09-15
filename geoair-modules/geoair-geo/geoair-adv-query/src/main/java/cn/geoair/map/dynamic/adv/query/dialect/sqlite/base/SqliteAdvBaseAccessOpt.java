package cn.geoair.map.dynamic.adv.query.dialect.sqlite.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.SqliteDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Supplier;

/** SQLite 插入操作。 */
public class SqliteAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public SqliteAdvBaseAccessOpt(
            Supplier<AdvQueryGlobalConfig> configGetter, AdvTypeHandlerRegistry registry) {
        super(configGetter, registry);
        this.dialectTableNameProcessor = SqliteDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildInsertIgnoreSql(
            String tableName,
            String fields,
            String placeholders,
            List<String> conflictKeys) {
        if (conflictKeys != null && !conflictKeys.isEmpty()) {
            return StrUtil.format(
                    "INSERT INTO {} ({}) VALUES ({}) ON CONFLICT ({}) DO NOTHING",
                    tableName, fields, placeholders, String.join(",", conflictKeys));
        }
        return StrUtil.format(
                "INSERT OR IGNORE INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
    }
}
