package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;

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
    protected String buildInsertIgnoreSql(String tableName, String fields, String placeholders, List<String> conflictKeys) {
        // Oracle 使用子查询判断：不存在则插入
        String[] fieldArray = fields.split(",");
        String pkField = fieldArray[0].trim();

        return StrUtil.format(
                "INSERT INTO {} ({}) SELECT {} FROM DUAL WHERE NOT EXISTS " +
                        "(SELECT 1 FROM {} WHERE {} = ?)",
                tableName, fields, placeholders, tableName, pkField);
    }


}
