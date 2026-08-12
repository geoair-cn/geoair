package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** MySQL 更新/Upsert 操作实现类 */
public class MysqlAdvBaseUpdateOpt extends AbstractExecAdvBaseUpdateOpt {

    public MysqlAdvBaseUpdateOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildUpsertFieldClause(String field) {
        return StrUtil.format("{} = VALUES({})", field, field);
    }

    @Override
    protected String buildUpdateOrInsertSql(
            String tableName, String fields, String placeholders,
            String conflictFields, String updateClause) {
        return StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({}) ON DUPLICATE KEY UPDATE {}",
                tableName, fields, placeholders, updateClause);
    }

    /**
     * MySQL 不支持多语句执行，UPDATE 批量用 PreparedStatement.addBatch。
     */
    @Override
    protected int executeUpdateBatch(Connection connection, List<Pair<String, List<Object>>> statements) throws SQLException {
        Map<String, List<List<Object>>> groups = new LinkedHashMap<>();
        for (Pair<String, List<Object>> stmt : statements) {
            groups.computeIfAbsent(stmt.getKey(), k -> new ArrayList<>()).add(stmt.getValue());
        }
        int total = 0;
        for (Map.Entry<String, List<List<Object>>> group : groups.entrySet()) {
            try (PreparedStatement pstmt = connection.prepareStatement(group.getKey())) {
                for (List<Object> params : group.getValue()) {
                    for (int i = 0; i < params.size(); i++) {
                        pstmt.setObject(i + 1, params.get(i));
                    }
                    pstmt.addBatch();
                }
                int[] results = pstmt.executeBatch();
                total += results.length;
            }
        }
        return total;
    }

    /**
     * MySQL 不支持多语句执行，Upsert 批量用 PreparedStatement.addBatch。
     */
    @Override
    protected int executeUpsertBatch(Connection connection, String tableName,
                                     List<Map<String, Object>> batchData, List<String> conflictKeys) throws SQLException {
        Map<String, List<List<Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> rowData : batchData) {
            Pair<String, List<Object>> upsertSql = getUpsertSql(tableName, rowData, conflictKeys);
            String sql = dialectTableNameProcessor.tbRemoveSqlSpaces(upsertSql.getKey());
            groups.computeIfAbsent(sql, k -> new ArrayList<>()).add(upsertSql.getValue());
        }
        int total = 0;
        for (Map.Entry<String, List<List<Object>>> group : groups.entrySet()) {
            try (PreparedStatement pstmt = connection.prepareStatement(group.getKey())) {
                for (List<Object> params : group.getValue()) {
                    for (int i = 0; i < params.size(); i++) {
                        pstmt.setObject(i + 1, params.get(i));
                    }
                    pstmt.addBatch();
                }
                int[] results = pstmt.executeBatch();
                total += results.length;
            }
        }
        return total;
    }
}
