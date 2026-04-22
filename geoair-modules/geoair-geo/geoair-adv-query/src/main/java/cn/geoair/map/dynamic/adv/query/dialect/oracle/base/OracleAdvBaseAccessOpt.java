package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Oracle插入操作实现类（简化版）
 *
 * @author zhangjun
 */
public class OracleAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public OracleAdvBaseAccessOpt() {
        this.dialectTableNameProcessor = OracleDialectTableNameUtil.getInstance();
    }



    @Override
    protected String buildInsertIgnoreSql(String tableName, String fields, String placeholders) {
        // Oracle 使用子查询判断：不存在则插入
        String[] fieldArray = fields.split(",");
        String pkField = fieldArray[0].trim();

        return StrUtil.format(
                "INSERT INTO {} ({}) SELECT {} FROM DUAL WHERE NOT EXISTS " +
                        "(SELECT 1 FROM {} WHERE {} = ?)",
                tableName, fields, placeholders, tableName, pkField);
    }

    @Override
    protected String buildInsertOrUpdateSql(
            String tableName, String fields, String placeholders, Set<String> updateFields) {
        // Oracle 使用 MERGE 语句
        String[] fieldArray = fields.split(",");
        String pkField = fieldArray[0].trim();

        String updateClause = updateFields.stream()
                .filter(f -> !f.equals(pkField))
                .map(f -> StrUtil.format("target.{} = source.{}", f, f))
                .collect(Collectors.joining(", "));

        return StrUtil.format(
                "MERGE INTO {} target USING (SELECT {} FROM DUAL) source ON (target.{} = source.{}) " +
                        "WHEN MATCHED THEN UPDATE SET {} " +
                        "WHEN NOT MATCHED THEN INSERT ({}) VALUES ({})",
                tableName, placeholders, pkField, pkField, updateClause, fields, placeholders);
    }
}
