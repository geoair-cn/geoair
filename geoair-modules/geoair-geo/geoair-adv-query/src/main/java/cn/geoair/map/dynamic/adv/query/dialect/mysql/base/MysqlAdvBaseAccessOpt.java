package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

/** MySQL插入操作实现类 仅实现MySQL专属的差异化语法，复用父类所有通用逻辑 */
public class MysqlAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public MysqlAdvBaseAccessOpt() {
        // 绑定MySQL专属的表名处理器
        this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
    }

    // MySQL默认主键字段
    private static final String MYSQL_DEFAULT_PRIMARY_KEY = "id";



    @Override
    protected String buildInsertIgnoreSql(String tableName, String fields, String placeholders) {
        return StrUtil.format(
                "INSERT IGNORE INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
    }

    @Override
    protected String buildInsertOrUpdateSql(
            String tableName, String fields, String placeholders, Set<String> updateFields) {
        // MySQL：ON DUPLICATE KEY UPDATE 语法
        String updateClause =
                updateFields
                        .stream()
                        .map(field -> StrUtil.format("{} = VALUES({})", field, field))
                        .collect(Collectors.joining(","));

        return StrUtil.format(
                "INSERT INTO {} ({}) VALUES ({}) ON DUPLICATE KEY UPDATE {}",
                tableName,
                fields,
                placeholders,
                updateClause);
    }


}
