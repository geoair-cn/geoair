package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;
import java.util.function.Supplier;

/** MySQL更新操作实现类 仅实现MySQL专属的差异化语法，复用父类所有通用逻辑 */
public class MysqlAdvBaseUpdateOpt extends AbstractExecAdvBaseUpdateOpt {

    public MysqlAdvBaseUpdateOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        // 绑定MySQL专属的表名处理器
        this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
    }

    // MySQL专属常量
    private static final String MYSQL_DUPLICATE_CLAUSE = " ON DUPLICATE KEY ";

    @Override
    protected String buildUpsertFieldClause(String field) {
        // MySQL：VALUES关键字引用插入值
        return StrUtil.format("{} = VALUES({})", field, field);
    }

    @Override
    protected String buildUpdateOrInsertSql(
            String tableName,
            String fields,
            String placeholders,
            String conflictFields,
            String updateClause) {
        // MySQL：ON DUPLICATE KEY UPDATE语法（无需指定冲突字段，依赖主键/唯一索引）
        return StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({}){}UPDATE SET {}",
                tableName,
                fields,
                placeholders,
                MYSQL_DUPLICATE_CLAUSE,
                updateClause);
    }
}
