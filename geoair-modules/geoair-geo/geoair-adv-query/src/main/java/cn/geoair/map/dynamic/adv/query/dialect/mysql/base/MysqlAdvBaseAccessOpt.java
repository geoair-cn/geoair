package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
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

/** MySQL 插入操作实现类 */
public class MysqlAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public MysqlAdvBaseAccessOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildInsertIgnoreSql(String tableName, String fields, String placeholders, List<String> conflictKeys) {
        return StrUtil.format(
                "INSERT IGNORE INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
    }

    /**
     * MySQL 不支持多语句执行，使用 PreparedStatement.addBatch 按 SQL 模板分组批量执行。
     */
    @Override
    protected int executeInsertIgnoreBatch(Connection connection, List<Pair<String, List<Object>>> statements) throws SQLException {
        Map<String, List<List<Object>>> groups = new LinkedHashMap<>();
        for (Pair<String, List<Object>> stmt : statements) {
            groups.computeIfAbsent(stmt.getKey(), k -> new ArrayList<>()).add(stmt.getValue());
        }
        int total = 0;
        for (Map.Entry<String, List<List<Object>>> group : groups.entrySet()) {
            try (PreparedStatement pstmt = connection.prepareStatement(group.getKey())) {
                for (List<Object> params : group.getValue()) {
                    bindParams(pstmt, params);
                    pstmt.addBatch();
                }
                int[] results = pstmt.executeBatch();
                total += results.length;
            }
        }
        return total;
    }

    private void bindParams(PreparedStatement pstmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }
}
