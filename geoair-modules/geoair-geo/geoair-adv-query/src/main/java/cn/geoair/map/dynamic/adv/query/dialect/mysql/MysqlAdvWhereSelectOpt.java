package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvWhereSelectOpt;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/16 15:46
 * @description： WhereSelect的操作实现类
 */
public class MysqlAdvWhereSelectOpt extends AbstractExecAdvWhereSelectOpt {

    protected IAdvBaseOpt baseOpt;
    protected IAdvSimplePagePreOpt simplePagePreOpt;
    protected IAdvGeoPreOpt iAdvGeoPreOpt;


    public MysqlAdvWhereSelectOpt(IDataSourceGetter dataSourceGetter,
                                  IAdvBaseOpt baseOpt,
                                  IAdvSimplePagePreOpt pgAdvSimplePageOpt,
                                  IAdvGeoPreOpt iAdvGeoPreOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.simplePagePreOpt = pgAdvSimplePageOpt;
        this.iAdvGeoPreOpt = iAdvGeoPreOpt;
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
    protected IAdvSimplePagePreOpt getSimplePageOpt() {
        return simplePagePreOpt;
    }

    @Override
    protected IAdvGeoPreOpt getGeoOpt() {
        return iAdvGeoPreOpt;
    }


}
