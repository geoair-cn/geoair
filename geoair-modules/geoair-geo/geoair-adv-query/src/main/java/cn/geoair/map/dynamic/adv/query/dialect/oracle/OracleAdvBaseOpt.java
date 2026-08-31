package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.base.OracleAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.base.OracleAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.base.OracleAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.base.OracleAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.db.dialect.DialectName;

import java.util.function.Supplier;

/**
 * Oracle数据库的动态高级查询基础操作实现类
 *
 * @author 张逢吉
 * @date 2025/10/9 10:16
 */
public class OracleAdvBaseOpt extends AbstractPxyAdvBaseOpt {

    private final AdvTypeHandlerRegistry typeHandlerRegistry;

    public OracleAdvBaseOpt(
            IDataSourceGetter dataSourceGetter,
            Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        super(dataSourceGetter, configAdvQueryGetter);
        this.typeHandlerRegistry =
                AdvTypeHandlerRegistry.create(
                        DialectName.ORACLE, configAdvQueryGetter.get().getTypeHandlers());
    }

    @Override
    public IAdvBaseAccessOpt getAdvBaseAccessPxyOpt() {
        if (advBaseAccessPxyOpt == null) {
            advBaseAccessPxyOpt = new OracleAdvBaseAccessOpt(this::getConfig, typeHandlerRegistry);
            advBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseAccessPxyOpt;
    }

    @Override
    public IAdvBaseSelectOpt getAdvBaseSelectPxyOpt() {
        if (advBaseSelectPxyOpt == null) {
            advBaseSelectPxyOpt = new OracleAdvBaseSelectOpt(this::getConfig, typeHandlerRegistry);
            advBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseSelectPxyOpt;
    }

    @Override
    public IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt() {
        if (advBaseUpdatePxyOpt == null) {
            advBaseUpdatePxyOpt = new OracleAdvBaseUpdateOpt(this::getConfig, typeHandlerRegistry);
            advBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseUpdatePxyOpt;
    }

    @Override
    public IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt() {
        if (advBaseDeletePxyOpt == null) {
            advBaseDeletePxyOpt = new OracleAdvBaseDeleteOpt(this::getConfig, typeHandlerRegistry);
            advBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return advBaseDeletePxyOpt;
    }
}
