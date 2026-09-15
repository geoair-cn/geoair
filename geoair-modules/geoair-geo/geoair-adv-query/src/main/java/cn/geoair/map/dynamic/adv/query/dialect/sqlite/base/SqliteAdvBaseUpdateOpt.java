package cn.geoair.map.dynamic.adv.query.dialect.sqlite.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.SqliteDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.function.Supplier;

/** SQLite 更新及 UPSERT 操作。 */
public class SqliteAdvBaseUpdateOpt extends AbstractExecAdvBaseUpdateOpt {

    public SqliteAdvBaseUpdateOpt(
            Supplier<AdvQueryGlobalConfig> configGetter, AdvTypeHandlerRegistry registry) {
        super(configGetter, registry);
        this.dialectTableNameProcessor = SqliteDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildUpsertFieldClause(String field) {
        return StrUtil.format("{} = excluded.{}", field, field);
    }

    @Override
    protected String buildUpdateOrInsertSql(
            String tableName,
            String fields,
            String placeholders,
            String conflictFields,
            String updateClause) {
        if (StrUtil.isBlank(updateClause)) {
            return StrUtil.format(
                    "INSERT INTO {} ({}) VALUES ({}) ON CONFLICT ({}) DO NOTHING",
                    tableName, fields, placeholders, conflictFields);
        }
        return StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({}) ON CONFLICT ({}) DO UPDATE SET {}",
                tableName, fields, placeholders, conflictFields, updateClause);
    }
}
