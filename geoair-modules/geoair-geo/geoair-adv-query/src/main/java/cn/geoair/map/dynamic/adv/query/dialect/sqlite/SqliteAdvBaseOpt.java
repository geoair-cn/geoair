package cn.geoair.map.dynamic.adv.query.dialect.sqlite;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.base.SqliteAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.base.SqliteAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.base.SqliteAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.base.SqliteAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.db.dialect.DialectName;

import java.util.function.Supplier;

/** SQLite 基础 CRUD 代理。 */
public class SqliteAdvBaseOpt extends AbstractPxyAdvBaseOpt {

    private final AdvTypeHandlerRegistry typeHandlerRegistry;

    public SqliteAdvBaseOpt(
            IDataSourceGetter dataSourceGetter,
            Supplier<AdvQueryGlobalConfig> configGetter) {
        super(dataSourceGetter, configGetter);
        this.typeHandlerRegistry = AdvTypeHandlerRegistry.create(
                DialectName.SQLITE3, configGetter.get().getTypeHandlers());
    }

    @Override
    public IAdvBaseAccessOpt getAdvBaseAccessPxyOpt() {
        if (advBaseAccessPxyOpt == null) {
            advBaseAccessPxyOpt = new SqliteAdvBaseAccessOpt(this::getConfig, typeHandlerRegistry);
            advBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseAccessPxyOpt;
    }

    @Override
    public IAdvBaseSelectOpt getAdvBaseSelectPxyOpt() {
        if (advBaseSelectPxyOpt == null) {
            advBaseSelectPxyOpt = new SqliteAdvBaseSelectOpt(this::getConfig, typeHandlerRegistry);
            advBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseSelectPxyOpt;
    }

    @Override
    public IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt() {
        if (advBaseUpdatePxyOpt == null) {
            advBaseUpdatePxyOpt = new SqliteAdvBaseUpdateOpt(this::getConfig, typeHandlerRegistry);
            advBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseUpdatePxyOpt;
    }

    @Override
    public IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt() {
        if (advBaseDeletePxyOpt == null) {
            advBaseDeletePxyOpt = new SqliteAdvBaseDeleteOpt(this::getConfig, typeHandlerRegistry);
            advBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseDeletePxyOpt;
    }
}
