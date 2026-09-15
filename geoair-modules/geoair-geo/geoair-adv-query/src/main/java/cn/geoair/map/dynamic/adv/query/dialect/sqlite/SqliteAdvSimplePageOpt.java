package cn.geoair.map.dynamic.adv.query.dialect.sqlite;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvSimplePageOpt;

/** SQLite 分页操作。 */
public class SqliteAdvSimplePageOpt extends AbstractExecAdvSimplePageOpt {

    private final IAdvBaseOpt baseOpt;
    private final IAdvGeoPreOpt geoOpt;
    private final IAdvDDLOpt ddlOpt;

    public SqliteAdvSimplePageOpt(
            IDataSourceGetter dataSourceGetter,
            IAdvBaseOpt baseOpt,
            IAdvGeoPreOpt geoOpt,
            IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.geoOpt = geoOpt;
        this.ddlOpt = ddlOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return SqliteDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        return ddlOpt;
    }

    @Override
    protected IAdvGeoPreOpt getAdvGeoPreOpt() {
        return geoOpt;
    }
}
