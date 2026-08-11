package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.base.MysqlAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.base.MysqlAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.base.MysqlAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.base.MysqlAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.db.dialect.DialectName;

import java.util.function.Supplier;

/**
 * @author 张逢吉
 * @date 2025/10/9 10:16
 */
public class MysqlAdvBaseOpt extends AbstractPxyAdvBaseOpt {

    private final AdvTypeHandlerRegistry typeHandlerRegistry;

    public MysqlAdvBaseOpt(IDataSourceGetter dataSourceGetter, Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(dataSourceGetter, configAdvQueryGetter);
        this.typeHandlerRegistry = AdvTypeHandlerRegistry.create(
                DialectName.MYSQL,
                configAdvQueryGetter.get().getTypeHandlers());
    }

    @Override
    public IAdvBaseAccessOpt getAdvBaseAccessPxyOpt() {
        if (advBaseAccessPxyOpt == null) {
            advBaseAccessPxyOpt = new MysqlAdvBaseAccessOpt(this::getConfig, typeHandlerRegistry);
            advBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseAccessPxyOpt;
    }

    @Override
    public IAdvBaseSelectOpt getAdvBaseSelectPxyOpt() {
        if (advBaseSelectPxyOpt == null) {
            advBaseSelectPxyOpt = new MysqlAdvBaseSelectOpt(this::getConfig, typeHandlerRegistry);
            advBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseSelectPxyOpt;
    }

    @Override
    public IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt() {
        if (advBaseUpdatePxyOpt == null) {
            advBaseUpdatePxyOpt = new MysqlAdvBaseUpdateOpt(this::getConfig, typeHandlerRegistry);
            advBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseUpdatePxyOpt;
    }

    @Override
    public IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt() {
        if (advBaseDeletePxyOpt == null) {
            advBaseDeletePxyOpt = new MysqlAdvBaseDeleteOpt(this::getConfig, typeHandlerRegistry);
            advBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseDeletePxyOpt;
    }
}
