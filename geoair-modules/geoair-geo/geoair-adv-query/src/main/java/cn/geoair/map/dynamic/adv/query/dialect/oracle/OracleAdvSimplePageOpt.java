package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvSimplePagePreOpt;

/**
 *Oracle带参数分页实现类
 */
public class OracleAdvSimplePageOpt extends AbstractExecAdvSimplePagePreOpt {

    protected static final GiLogger log = GirLogger.getLoger();

    // PG专属的依赖类（复用父类已初始化的）
    protected IAdvGeoPreOpt advGeoPreOpt;

    protected IAdvBaseOpt baseOpt;

    protected IAdvDDLOpt advDDLOpt;

    public OracleAdvSimplePageOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvGeoPreOpt advGeoPreOpt, IAdvDDLOpt advDDLOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.advDDLOpt = advDDLOpt;
        this.advGeoPreOpt = advGeoPreOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return OracleDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        return advDDLOpt;
    }

    @Override
    protected IAdvGeoPreOpt getAdvGeoPreOpt() {
        return advGeoPreOpt;
    }

}
