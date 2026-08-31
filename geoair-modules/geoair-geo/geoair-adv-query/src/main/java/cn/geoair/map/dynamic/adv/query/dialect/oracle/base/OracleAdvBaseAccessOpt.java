package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Supplier;

/**
 * Oracle插入操作实现类（简化版）
 *
 * @author zhangjun
 */
public class OracleAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public OracleAdvBaseAccessOpt(
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = OracleDialectTableNameUtil.getInstance();
    }

    @Override
    protected boolean needsConflictKeyParams() {
        return true;
    }

    @Override
    protected String buildInsertIgnoreSql(
            String tableName, String fields, String placeholders, List<String> conflictKeys) {
        if (conflictKeys == null || conflictKeys.isEmpty()) {
            conflictKeys = java.util.Collections.singletonList(fields.split(",")[0].trim());
        }
        String conflictCondition =
                conflictKeys.stream()
                        .map(ck -> StrUtil.format("{} = ?", ck))
                        .collect(java.util.stream.Collectors.joining(" AND "));

        return StrUtil.format(
                "INSERT INTO {} ({}) SELECT {} FROM DUAL WHERE NOT EXISTS "
                        + "(SELECT 1 FROM {} WHERE {})",
                tableName,
                fields,
                placeholders,
                tableName,
                conflictCondition);
    }
}
