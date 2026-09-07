package cn.geoair.map.dynamic.adv.query.dialect.sqlserver.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.SqlServerDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * SQL Server 的 UPSERT 实现。@author 张逢吉
 */
public class SqlServerAdvBaseUpdateOpt extends AbstractExecAdvBaseUpdateOpt {

    public SqlServerAdvBaseUpdateOpt(
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = SqlServerDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildUpsertFieldClause(String field) {
        return "target." + field + " = source." + field;
    }

    @Override
    protected String buildUpdateOrInsertSql(
            String tableName, String fields, String placeholders, String conflictFields, String updateClause) {
        String onClause = Arrays.stream(conflictFields.split(","))
                .map(String::trim)
                .map(field -> "target." + field + " = source." + field)
                .collect(Collectors.joining(" AND "));
        String sourceValues = Arrays.stream(fields.split(","))
                .map(String::trim)
                .map(field -> "source." + field)
                .collect(Collectors.joining(","));
        String matchedClause = StrUtil.isBlank(updateClause) ? "" : "WHEN MATCHED THEN UPDATE SET " + updateClause + " ";
        return "MERGE INTO " + tableName + " WITH (HOLDLOCK) AS target "
               + "USING (VALUES (" + placeholders + ")) AS source (" + fields + ") ON " + onClause + " "
               + matchedClause
               + "WHEN NOT MATCHED THEN INSERT (" + fields + ") VALUES (" + sourceValues + ");";
    }
}
