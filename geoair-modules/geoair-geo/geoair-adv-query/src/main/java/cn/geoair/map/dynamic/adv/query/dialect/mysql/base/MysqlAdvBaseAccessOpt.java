package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.function.Supplier;

/** MySQL 插入操作实现类 */
public class MysqlAdvBaseAccessOpt extends AbstractExecAdvBaseAccessOpt {

    public MysqlAdvBaseAccessOpt(
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildInsertIgnoreSql(
            String tableName, String fields, String placeholders, List<String> conflictKeys) {
        return StrUtil.format(
                "INSERT IGNORE INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
    }
}
