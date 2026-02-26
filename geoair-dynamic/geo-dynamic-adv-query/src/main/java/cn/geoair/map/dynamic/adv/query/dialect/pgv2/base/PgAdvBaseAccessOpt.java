package cn.geoair.map.dynamic.adv.query.dialect.pgv2.base;


import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvBaseAccessOpt;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PostgreSQL插入操作实现类
 * 仅实现PG专属的差异化语法，复用父类所有通用逻辑
 */
public class PgAdvBaseAccessOpt extends AbstractAdvBaseAccessOpt {
    // PG专属常量
    private static final String PG_CONFLICT_CLAUSE = " ON CONFLICT DO ";
    // PG默认主键字段
    private static final String PG_DEFAULT_PRIMARY_KEY = "id";

    // ========== 实现差异化抽象方法 ==========
    @Override
    protected String buildInsertReturnIdSql(String tableName, String fields, String placeholders) {
        // PG：RETURNING 主键语法
        return StrUtil.format("INSERT INTO {} ({}) VALUES ({}) RETURNING {}",
                tableName, fields, placeholders, PG_DEFAULT_PRIMARY_KEY);
    }

    @Override
    protected Long executeInsertReturnId(Connection connection, String execSql, Object... params) throws SQLException {
        // PG：执行并获取自增主键
        return SqlExecutor.executeForGeneratedKey(connection, execSql, params).longValue();
    }

    @Override
    protected String buildInsertIgnoreSql(String tableName, String fields, String placeholders) {
        // PG：ON CONFLICT DO NOTHING
        return StrUtil.format("INSERT INTO {} ({}) VALUES ({}){}NOTHING",
                tableName, fields, placeholders, PG_CONFLICT_CLAUSE);
    }

    @Override
    protected String buildInsertOrUpdateSql(String tableName, String fields, String placeholders, Set<String> updateFields) {
        // PG：ON CONFLICT DO UPDATE + EXCLUDED关键字
        String updateClause = updateFields.stream()
                .map(field -> StrUtil.format("{} = EXCLUDED.{}", field, field))
                .collect(Collectors.joining(","));

        return StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({}){}UPDATE SET {}",
                tableName, fields, placeholders, PG_CONFLICT_CLAUSE, updateClause
        );
    }
}
