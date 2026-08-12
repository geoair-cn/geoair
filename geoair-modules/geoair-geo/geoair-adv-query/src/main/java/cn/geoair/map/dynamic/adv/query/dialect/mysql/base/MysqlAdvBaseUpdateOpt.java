package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.function.Supplier;

/** MySQL 更新/Upsert 操作实现类 */
public class MysqlAdvBaseUpdateOpt extends AbstractExecAdvBaseUpdateOpt {

    public MysqlAdvBaseUpdateOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildUpsertFieldClause(String field) {
        return StrUtil.format("{} = VALUES({})", field, field);
    }

    @Override
    protected String buildUpdateOrInsertSql(
            String tableName, String fields, String placeholders,
            String conflictFields, String updateClause) {
        return StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({}) ON DUPLICATE KEY UPDATE {}",
                tableName, fields, placeholders, updateClause);
    }
}
