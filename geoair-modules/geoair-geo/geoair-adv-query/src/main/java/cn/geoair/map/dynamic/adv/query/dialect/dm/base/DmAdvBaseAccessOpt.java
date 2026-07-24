package cn.geoair.map.dynamic.adv.query.dialect.dm.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.DmDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦插入操作实现类
 */
public class DmAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public DmAdvBaseAccessOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        this.dialectTableNameProcessor = DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildInsertIgnoreSql(
            String tableName, String fields, String placeholders, List<String> conflictKeys) {
        String[] fieldArray = fields.split(",");
        StringBuilder sourceSelectBuilder = new StringBuilder("SELECT ");
        for (int i = 0; i < fieldArray.length; i++) {
            String field = fieldArray[i].trim();
            if (i > 0) {
                sourceSelectBuilder.append(", ");
            }
            sourceSelectBuilder.append("? AS ").append(field);
        }
        sourceSelectBuilder.append(" FROM DUAL");

        List<String> effectiveConflictKeys = conflictKeys;
        if (effectiveConflictKeys == null || effectiveConflictKeys.isEmpty()) {
            effectiveConflictKeys = java.util.Collections.singletonList(fieldArray[0].trim());
        }
        String conflictCondition =
                effectiveConflictKeys
                        .stream()
                        .map(
                                field -> {
                                    String trimmedField = field.trim();
                                    return StrUtil.format(
                                            "target.{} = source.{}", trimmedField, trimmedField);
                                })
                        .collect(java.util.stream.Collectors.joining(" AND "));
        return StrUtil.format(
                "INSERT INTO {} ({}) SELECT * FROM ({}) source WHERE NOT EXISTS (SELECT 1 FROM {} target WHERE {})",
                tableName,
                fields,
                sourceSelectBuilder,
                tableName,
                conflictCondition);
    }
}
