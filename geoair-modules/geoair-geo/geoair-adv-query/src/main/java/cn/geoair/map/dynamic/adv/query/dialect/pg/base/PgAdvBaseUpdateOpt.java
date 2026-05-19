package cn.geoair.map.dynamic.adv.query.dialect.pg.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;
import java.util.function.Supplier;

/** PostgreSQL更新操作实现类 仅实现PG专属的差异化语法，复用父类所有通用逻辑 */
public class PgAdvBaseUpdateOpt extends AbstractExecAdvBaseUpdateOpt {

    public PgAdvBaseUpdateOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        // 绑定MySQL专属的表名处理器
        this.dialectTableNameProcessor = PgDialectTableNameUtil.getInstance();
    }

    // PG专属常量
    private static final String PG_CONFLICT_CLAUSE = " ON CONFLICT ";

    @Override
    protected String buildUpsertFieldClause(String field) {
        // PG：EXCLUDED关键字引用插入值
        return StrUtil.format("{} = EXCLUDED.{}", field, field);
    }

    @Override
    protected String buildUpdateOrInsertSql(
            String tableName,
            String fields,
            String placeholders,
            String conflictFields,
            String updateClause) {
        // PG：ON CONFLICT DO UPDATE语法
        return StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({}){}({}) DO UPDATE SET {}",
                tableName,
                fields,
                placeholders,
                PG_CONFLICT_CLAUSE,
                conflictFields,
                updateClause);
    }
}
