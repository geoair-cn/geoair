package cn.geoair.map.dynamic.adv.query.dialect.pg.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;

import java.util.function.Supplier;

/** PostgreSQL删除操作实现类 */
public class PgAdvBaseDeleteOpt extends AbstractExecAdvBaseDeleteOpt {

    public PgAdvBaseDeleteOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter, AdvTypeHandlerRegistry registry) {
        super(configAdvQueryGetter, registry);
        // 绑定MySQL专属的表名处理器
        this.dialectTableNameProcessor = PgDialectTableNameUtil.getInstance();
    }

    // PG专属常量
    private static final int PG_MAX_IN_PARAMS = 1000;

    // ========== 实现差异化抽象方法 ==========
    @Override
    protected int getMaxInParams() {
        return PG_MAX_IN_PARAMS;
    }
}
