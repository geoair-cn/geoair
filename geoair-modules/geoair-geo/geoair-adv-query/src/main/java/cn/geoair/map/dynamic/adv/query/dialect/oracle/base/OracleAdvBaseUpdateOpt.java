package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * PostgreSQL更新操作实现类 仅实现PG专属的差异化语法，复用父类所有通用逻辑
 */
public class OracleAdvBaseUpdateOpt extends AbstractExecAdvBaseUpdateOpt {

    public OracleAdvBaseUpdateOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        // 绑定MySQL专属的表名处理器
        this.dialectTableNameProcessor = OracleDialectTableNameUtil.getInstance();
    }

    // PG专属常量
    private static final String PG_CONFLICT_CLAUSE = " ON CONFLICT ";

    /**
     * Oracle UPSERT 字段更新子句
     * <p>Oracle MERGE 语法中，更新部分直接使用目标表字段名</p>
     */
    @Override
    protected String buildUpsertFieldClause(String field) {
        return StrUtil.format("target.{} = source.{}", field, field);
    }

    /**
     * Oracle UPSERT SQL（使用 MERGE 语句）
     * <p>
     * Oracle MERGE 语法：
     * MERGE INTO table_name target
     * USING (SELECT ? AS col1, ? AS col2 FROM DUAL) source
     * ON (target.id = source.id)
     * WHEN MATCHED THEN UPDATE SET target.col1 = source.col1, target.col2 = source.col2
     * WHEN NOT MATCHED THEN INSERT (col1, col2) VALUES (source.col1, source.col2)
     * </p>
     */
    @Override
    protected String buildUpdateOrInsertSql(String tableName,
                                            String fields,
                                            String placeholders,
                                            String conflictFields,
                                            String updateClause) {
        String[] fieldArray = fields.split(",");

        // USING 子句
        StringBuilder usingBuilder = new StringBuilder("SELECT ");
        for (int i = 0; i < fieldArray.length; i++) {
            if (i > 0) usingBuilder.append(", ");
            usingBuilder.append("? AS ").append(fieldArray[i].trim());
        }
        usingBuilder.append(" FROM DUAL");
        String usingClause = usingBuilder.toString();

        // INSERT 引用 source 列，不复用 ? 占位符
        String insertValues = java.util.Arrays.stream(fieldArray)
                .map(f -> "source." + f.trim())
                .collect(Collectors.joining(", "));

        // ON 条件
        String[] conflictFieldArray = conflictFields.split(",");
        String onCondition = java.util.Arrays.stream(conflictFieldArray)
                .map(f -> StrUtil.format("target.{} = source.{}", f.trim(), f.trim()))
                .collect(Collectors.joining(" AND "));

        return StrUtil.format(
                "MERGE INTO {} target USING ({}) source ON ({}) " +
                        "WHEN MATCHED THEN UPDATE SET {} " +
                        "WHEN NOT MATCHED THEN INSERT ({}) VALUES ({})",
                tableName, usingClause, onCondition, updateClause, fields, insertValues);
    }

    /**
     * Oracle 批量更新优化（使用 FORALL）
     * <p>可选实现，用于提升批量更新性能</p>
     */
    public String buildBatchUpdateWithForall(String tableName,
                                             String idKey,
                                             Set<String> updateFields,
                                             int batchSize) {
        String quotedIdKey = dialectTableNameProcessor.tbQuoteFieldName(idKey);
        // 构建 FORALL 批量更新语句
        String setClause = updateFields.stream()
                .map(field -> {
                    String quotedField = dialectTableNameProcessor.tbQuoteFieldName(field);
                    return StrUtil.format("{} = {}_new.{}", quotedField, tableName, quotedField);
                })
                .collect(Collectors.joining(", "));

        String fieldList = updateFields.stream()
                .map(field -> {
                    String quotedField = dialectTableNameProcessor.tbQuoteFieldName(field);
                    return StrUtil.format("{}_new.{}", tableName, quotedField);
                })
                .collect(Collectors.joining(", "));

        return StrUtil.format(
                "DECLARE\n" +
                        "  TYPE {}_id_type IS TABLE OF {}%ROWTYPE INDEX BY PLS_INTEGER;\n" +
                        "  l_data {}_id_type;\n" +
                        "BEGIN\n" +
                        "  FORALL i IN 1..? \n" +
                        "    UPDATE {} SET {} WHERE {} = l_data(i).{};\n" +
                        "END;",
                tableName, tableName, tableName, tableName, setClause, quotedIdKey, quotedIdKey);
    }
}
