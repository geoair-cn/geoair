package cn.geoair.map.dynamic.adv.query.dialect.dm.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.DmDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
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

    public DmAdvBaseUpdateOpt(
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildUpsertFieldClause(String field) {
        return StrUtil.format("target.{} = source.{}", field, field);
    }

    @Override
    protected String buildUpdateOrInsertSql(
            String tableName,
            String fields,
            String placeholders,
            String conflictFields,
            String updateClause) {
        String[] fieldArray = fields.split(",");
        StringBuilder usingBuilder = new StringBuilder("SELECT ");
        StringBuilder insertValueBuilder = new StringBuilder();
        for (int i = 0; i < fieldArray.length; i++) {
            String field = fieldArray[i].trim();
            if (i > 0) {
                usingBuilder.append(", ");
                insertValueBuilder.append(", ");
            }
            usingBuilder.append("? AS ").append(field);
            insertValueBuilder.append("source.").append(field);
        }
        usingBuilder.append(" FROM DUAL");
        String usingClause = usingBuilder.toString();
        String insertValues = insertValueBuilder.toString();
        String[] conflictFieldArray = conflictFields.split(",");
        String onCondition =
                java.util.Arrays.stream(conflictFieldArray)
                        .map(
                                field ->
                                        StrUtil.format(
                                                "target.{} = source.{}",
                                                field.trim(),
                                                field.trim()))
                        .collect(Collectors.joining(" AND "));
        if (StrUtil.isBlank(updateClause)) {
            String firstConflictField = conflictFieldArray[0].trim();
            updateClause =
                    StrUtil.format("target.{} = source.{}", firstConflictField, firstConflictField);
        }
        return StrUtil.format(
                "MERGE INTO {} target USING ({}) source ON ({}) WHEN MATCHED THEN UPDATE SET {} WHEN NOT MATCHED THEN INSERT ({}) VALUES ({})",
                tableName,
                usingClause,
                onCondition,
                updateClause,
                fields,
                insertValues);
    }

    public String buildBatchUpdateWithForall(
            String tableName, String idKey, Set<String> updateFields, int batchSize) {
        throw new UnsupportedOperationException("DM 暂未实现 buildBatchUpdateWithForall 的可执行批量更新语句");
    }
}
