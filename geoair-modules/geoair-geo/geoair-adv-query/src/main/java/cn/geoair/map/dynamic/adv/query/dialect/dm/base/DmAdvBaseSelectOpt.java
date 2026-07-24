package cn.geoair.map.dynamic.adv.query.dialect.dm.base;

import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.DmDialectTableNameUtil;
import cn.hutool.core.util.StrUtil;

import java.util.function.Supplier;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦查询操作实现类
 */
public class DmAdvBaseSelectOpt extends AbstractExecAdvBaseSelectOpt {

    public DmAdvBaseSelectOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(configAdvQueryGetter);
        this.dialectTableNameProcessor = DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected String buildSelectOneWrapSql(String cleanSql) {
        return StrUtil.format("SELECT * FROM ({}) WHERE ROWNUM = 1", cleanSql);
    }

    @Override
    protected String buildCountQuerySql(String cleanSql) {
        return StrUtil.format("SELECT COUNT(1) FROM ({})", cleanSql);
    }
}
