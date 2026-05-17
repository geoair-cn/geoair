package cn.geoair.map.dynamic.adv.query.dialect.oracle.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.function.Supplier;

/**
 * Oracle插入操作实现类（简化版）
 *
 * @author zhangjun
 */
public class OracleAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public OracleAdvBaseAccessOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(configAdvQueryGetter);
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
