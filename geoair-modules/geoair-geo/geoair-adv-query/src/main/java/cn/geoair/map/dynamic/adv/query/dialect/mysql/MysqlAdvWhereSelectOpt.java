package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvWhereSelectOpt;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/16 15:46
 * @description： TODO
 */
public class MysqlAdvWhereSelectOpt extends AbstractExecAdvWhereSelectOpt {
    protected IAdvBaseOpt baseOpt;
    protected IAdvSimplePagePreOpt simplePagePreOpt;


    public MysqlAdvWhereSelectOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvSimplePagePreOpt pgAdvSimplePageOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.simplePagePreOpt = pgAdvSimplePageOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return MysqlDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseSelectOpt getBaseSelectOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvSimplePageOpt getSimplePageOpt() {
        return simplePagePreOpt;
    }
}
