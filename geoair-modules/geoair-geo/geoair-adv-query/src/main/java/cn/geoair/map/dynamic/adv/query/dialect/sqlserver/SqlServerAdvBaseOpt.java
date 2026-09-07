package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.base.SqlServerAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.base.SqlServerAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.base.SqlServerAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.base.SqlServerAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.db.dialect.DialectName;

import java.util.function.Supplier;

/** SQL Server 基础 CRUD 聚合器。@author 张逢吉 */
public class SqlServerAdvBaseOpt extends AbstractPxyAdvBaseOpt {
    private final AdvTypeHandlerRegistry typeHandlerRegistry;

    public SqlServerAdvBaseOpt(
            IDataSourceGetter dataSourceGetter, Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(dataSourceGetter, configAdvQueryGetter);
        this.typeHandlerRegistry = AdvTypeHandlerRegistry.create(
                DialectName.SQLSERVER, configAdvQueryGetter.get().getTypeHandlers());
    }

    @Override public IAdvBaseAccessOpt getAdvBaseAccessPxyOpt() {
        if (advBaseAccessPxyOpt == null) {
            advBaseAccessPxyOpt = new SqlServerAdvBaseAccessOpt(this::getConfig, typeHandlerRegistry);
            advBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseAccessPxyOpt;
    }
    @Override public IAdvBaseSelectOpt getAdvBaseSelectPxyOpt() {
        if (advBaseSelectPxyOpt == null) {
            advBaseSelectPxyOpt = new SqlServerAdvBaseSelectOpt(this::getConfig, typeHandlerRegistry);
            advBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseSelectPxyOpt;
    }
    @Override public IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt() {
        if (advBaseUpdatePxyOpt == null) {
            advBaseUpdatePxyOpt = new SqlServerAdvBaseUpdateOpt(this::getConfig, typeHandlerRegistry);
            advBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseUpdatePxyOpt;
    }
    @Override public IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt() {
        if (advBaseDeletePxyOpt == null) {
            advBaseDeletePxyOpt = new SqlServerAdvBaseDeleteOpt(this::getConfig, typeHandlerRegistry);
            advBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseDeletePxyOpt;
    }
}
