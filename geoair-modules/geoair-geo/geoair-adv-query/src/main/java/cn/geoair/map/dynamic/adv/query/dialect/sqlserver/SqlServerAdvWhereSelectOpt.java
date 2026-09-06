package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.IAdvSimplePageOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvWhereSelectOpt;

/** SQL Server 条件查询实现。@author 张逢吉 */
public class SqlServerAdvWhereSelectOpt extends AbstractExecAdvWhereSelectOpt {
    private final IAdvBaseOpt baseOpt;
    private final IAdvSimplePageOpt pageOpt;
    private final IAdvGeoPreOpt geoOpt;
    public SqlServerAdvWhereSelectOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvSimplePageOpt pageOpt, IAdvGeoPreOpt geoOpt) {
        super(dataSourceGetter); this.baseOpt = baseOpt; this.pageOpt = pageOpt; this.geoOpt = geoOpt;
    }
    @Override protected DialectTableNameProcessor getDialectTableNameProcessor() { return SqlServerDialectTableNameUtil.getInstance(); }
    @Override protected IAdvBaseSelectOpt getBaseSelectOpt() { return baseOpt; }
    @Override protected IAdvSimplePageOpt getSimplePageOpt() { return pageOpt; }
    @Override protected IAdvGeoPreOpt getGeoOpt() { return geoOpt; }
}
