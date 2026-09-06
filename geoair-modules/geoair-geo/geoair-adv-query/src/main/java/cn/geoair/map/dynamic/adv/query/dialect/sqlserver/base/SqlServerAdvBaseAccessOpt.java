package cn.geoair.map.dynamic.adv.query.dialect.sqlserver.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.SqlServerDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Supplier;

/**
 * SQL Server 插入操作实现。
 *
 * <p>SQL Server 没有 MySQL 的 {@code INSERT IGNORE}。有明确冲突字段时使用
 * {@code MERGE ... WHEN NOT MATCHED} 实现“存在则忽略”；没有冲突字段时保持普通插入，
 * 以免悄悄吞掉未知的约束异常。
 *
 * @author 张逢吉
 */
public class SqlServerAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public SqlServerAdvBaseAccessOpt(
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = SqlServerDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildInsertIgnoreSql(
            String tableName, String fields, String placeholders, List<String> conflictKeys) {
        if (conflictKeys == null || conflictKeys.isEmpty()) {
            return StrUtil.format("INSERT INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
        }
        String sourceFields = fields;
        String onClause = conflictKeys.stream()
                .map(field -> "target." + field + " = source." + field)
                .collect(java.util.stream.Collectors.joining(" AND "));
        String sourceValues = java.util.Arrays.stream(fields.split(","))
                .map(field -> "source." + field.trim())
                .collect(java.util.stream.Collectors.joining(","));
        return StrUtil.format(
                "MERGE INTO {} WITH (HOLDLOCK) AS target USING (VALUES ({})) AS source ({}) ON {} "
                        + "WHEN NOT MATCHED THEN INSERT ({}) VALUES ({});",
                tableName, placeholders, sourceFields, onClause, fields, sourceValues);
    }
}
