package cn.geoair.map.dynamic.adv.query.dialect.sqlite;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.IAdvSimplePageOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvWhereSelectOpt;

/** SQLite 动态条件查询。 */
public class SqliteAdvWhereSelectOpt extends AbstractExecAdvWhereSelectOpt {

    private final IAdvBaseOpt baseOpt;
    private final IAdvSimplePageOpt pageOpt;
    private final IAdvGeoPreOpt geoOpt;

    public SqliteAdvWhereSelectOpt(
            IDataSourceGetter dataSourceGetter,
            IAdvBaseOpt baseOpt,
            IAdvSimplePageOpt pageOpt,
            IAdvGeoPreOpt geoOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.pageOpt = pageOpt;
        this.geoOpt = geoOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return SqliteDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseSelectOpt getBaseSelectOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvSimplePageOpt getSimplePageOpt() {
        return pageOpt;
    }

    @Override
    protected IAdvGeoPreOpt getGeoOpt() {
        return geoOpt;
    }
}
