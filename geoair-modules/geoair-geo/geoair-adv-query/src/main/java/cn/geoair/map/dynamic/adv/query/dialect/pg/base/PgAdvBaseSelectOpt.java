package cn.geoair.map.dynamic.adv.query.dialect.pg.base;

import cn.geoair.map.dynamic.adv.config.ConfigAdvQuery;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;

import java.util.function.Supplier;

/** PostgreSQL查询操作实现类 */
public class PgAdvBaseSelectOpt extends AbstractExecAdvBaseSelectOpt {

    // 初始化表名处理器
    public PgAdvBaseSelectOpt(Supplier<ConfigAdvQuery> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        this.dialectTableNameProcessor = PgDialectTableNameUtil.getInstance();
    }
}
