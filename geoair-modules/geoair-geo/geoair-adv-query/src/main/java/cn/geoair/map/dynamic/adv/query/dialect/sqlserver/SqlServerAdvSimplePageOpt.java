package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvSimplePageOpt;

/** SQL Server {@code OFFSET/FETCH} 分页实现。@author 张逢吉 */
public class SqlServerAdvSimplePageOpt extends AbstractExecAdvSimplePageOpt {
    private final IAdvBaseOpt baseOpt;
    private final IAdvGeoPreOpt geoOpt;
    private final IAdvDDLOpt ddlOpt;
    public SqlServerAdvSimplePageOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvGeoPreOpt geoOpt, IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter); this.baseOpt = baseOpt; this.geoOpt = geoOpt; this.ddlOpt = ddlOpt;
    }
    @Override protected DialectTableNameProcessor getDialectTableNameProcessor() { return SqlServerDialectTableNameUtil.getInstance(); }
    @Override protected IAdvBaseOpt getAdvBaseOpt() { return baseOpt; }
    @Override protected IAdvDDLOpt getAdvDDLOpt() { return ddlOpt; }
    @Override protected IAdvGeoPreOpt getAdvGeoPreOpt() { return geoOpt; }
}
