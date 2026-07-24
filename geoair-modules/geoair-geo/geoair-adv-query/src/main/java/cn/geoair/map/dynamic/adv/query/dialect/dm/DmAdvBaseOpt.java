package cn.geoair.map.dynamic.adv.query.dialect.dm;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.base.DmAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.base.DmAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.base.DmAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.dm.base.DmAdvBaseUpdateOpt;
import java.util.function.Supplier;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦数据库的动态高级查询基础操作实现类
 */
public class DmAdvBaseOpt extends AbstractPxyAdvBaseOpt {

    public DmAdvBaseOpt(
            IDataSourceGetter dataSourceGetter,
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(dataSourceGetter, configAdvQueryGetter);
    }

    @Override
    public IAdvBaseAccessOpt getAdvBaseAccessPxyOpt() {
        if (advBaseAccessPxyOpt == null) {
            advBaseAccessPxyOpt = new DmAdvBaseAccessOpt(this::getConfig);
            advBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseAccessPxyOpt;
    }

    @Override
    public IAdvBaseSelectOpt getAdvBaseSelectPxyOpt() {
        if (advBaseSelectPxyOpt == null) {
            advBaseSelectPxyOpt = new DmAdvBaseSelectOpt(this::getConfig);
            advBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseSelectPxyOpt;
    }

    @Override
    public IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt() {
        if (advBaseUpdatePxyOpt == null) {
            advBaseUpdatePxyOpt = new DmAdvBaseUpdateOpt(this::getConfig);
            advBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseUpdatePxyOpt;
    }

    @Override
    public IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt() {
        if (advBaseDeletePxyOpt == null) {
            advBaseDeletePxyOpt = new DmAdvBaseDeleteOpt(this::getConfig);
            advBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseDeletePxyOpt;
    }
}
