package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvSimplePageOpt;

/**
 * MySQL 带参数分页实现类
 */
public class MysqlAdvSimplePageOpt extends AbstractExecAdvSimplePageOpt {

    // MySQL专属依赖
    protected IAdvGeoPreOpt mysqlAdvGeoPreOpt;

    protected IAdvBaseOpt baseOpt;

    protected IAdvDDLOpt mysqlAdvDDLOpt;

    public MysqlAdvSimplePageOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvGeoPreOpt mysqlAdvGeoOpt, IAdvDDLOpt mysqlAdvDDLOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.mysqlAdvDDLOpt = mysqlAdvDDLOpt;
        this.mysqlAdvGeoPreOpt = mysqlAdvGeoOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return MysqlDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        return mysqlAdvDDLOpt;
    }

    @Override
    protected IAdvGeoPreOpt getAdvGeoPreOpt() {
        return mysqlAdvGeoPreOpt;
    }


}
