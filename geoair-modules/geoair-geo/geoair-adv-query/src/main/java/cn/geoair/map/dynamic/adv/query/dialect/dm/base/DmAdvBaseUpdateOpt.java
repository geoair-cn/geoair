package cn.geoair.map.dynamic.adv.query.dialect.dm.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.DmDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦更新操作实现类
 */
public class DmAdvBaseUpdateOpt extends AbstractExecAdvBaseUpdateOpt {

    public DmAdvBaseUpdateOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        this.dialectTableNameProcessor = DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildUpsertFieldClause(String field) {
        return StrUtil.format("{} = VALUES({})", field, field);
    }

    @Override
    protected String buildUpdateOrInsertSql(String tableName,
                                            String fields,
                                            String placeholders,
                                            String conflictFields,
                                            String updateClause) {
        String[] fieldArray = fields.split(",");
        StringBuilder usingBuilder = new StringBuilder("SELECT ");
        for (int i = 0; i < fieldArray.length; i++) {
            if (i > 0) {
                usingBuilder.append(", ");
            }
            usingBuilder.append("? AS ").append(fieldArray[i].trim());
        }
        String usingClause = usingBuilder.toString();
        String[] conflictFieldArray = conflictFields.split(",");
        String onCondition = java.util.Arrays.stream(conflictFieldArray)
                .map(field -> StrUtil.format("target.{} = source.{}", field.trim(), field.trim()))
                .collect(Collectors.joining(" AND "));
        return StrUtil.format(
                "MERGE INTO {} target USING ({}) source ON ({}) WHEN MATCHED THEN UPDATE SET {} WHEN NOT MATCHED THEN INSERT ({}) VALUES ({})",
                tableName, usingClause, onCondition, updateClause, fields, placeholders);
    }

    public String buildBatchUpdateWithForall(String tableName,
                                             String idKey,
                                             Set<String> updateFields,
                                             int batchSize) {
        String setClause = updateFields.stream()
                .map(field -> StrUtil.format("{} = {}_new.{}", field, tableName, field))
                .collect(Collectors.joining(", "));
        return StrUtil.format(
                "BEGIN FOR i IN 1..{} LOOP UPDATE {} SET {} WHERE {} = ?; END LOOP; END;",
                batchSize, tableName, setClause, idKey);
    }
}
