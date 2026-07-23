package cn.geoair.map.dynamic.adv.query.dialect.dm;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.IAdvSimplePageOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvWhereSelectOpt;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦条件查询实现类
 */
public class DmAdvWhereSelectOpt extends AbstractExecAdvWhereSelectOpt {

    protected IAdvBaseOpt baseOpt;
    protected IAdvSimplePageOpt simplePagePreOpt;
    protected IAdvGeoPreOpt iAdvGeoPreOpt;

    public DmAdvWhereSelectOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt,
                               IAdvSimplePageOpt dmAdvSimplePageOpt, IAdvGeoPreOpt iAdvGeoPreOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.simplePagePreOpt = dmAdvSimplePageOpt;
        this.iAdvGeoPreOpt = iAdvGeoPreOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseSelectOpt getBaseSelectOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvSimplePageOpt getSimplePageOpt() {
        return simplePagePreOpt;
    }

    @Override
    protected IAdvGeoPreOpt getGeoOpt() {
        return iAdvGeoPreOpt;
    }
}
