package cn.geoair.map.dynamic.adv.query.dialect.dm.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.DmDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦插入操作实现类
 */
public class DmAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public DmAdvBaseAccessOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        this.dialectTableNameProcessor = DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildInsertIgnoreSql(String tableName, String fields, String placeholders, List<String> conflictKeys) {
        String[] fieldArray = fields.split(",");
        String pkField = fieldArray[0].trim();
        return StrUtil.format(
                "INSERT INTO {} ({}) SELECT {} FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM {} WHERE {} = ?)",
                tableName, fields, placeholders, tableName, pkField);
    }
}
