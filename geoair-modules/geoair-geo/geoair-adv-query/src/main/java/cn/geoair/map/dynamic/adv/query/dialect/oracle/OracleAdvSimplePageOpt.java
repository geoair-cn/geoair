package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvSimplePagePreOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsKeyTran;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import cn.geoair.map.dynamic.adv.query.utils.GirAdvQueryCommonUtils;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Objects;

/**
 * Oracle带参数分页实现类
 */
public class OracleAdvSimplePageOpt extends AbstractExecAdvSimplePagePreOpt {

    protected static final GiLogger log = GirLogger.getLoger();

    // Oracle专属的依赖类
    protected IAdvGeoPreOpt advGeoPreOpt;
    protected IAdvBaseOpt baseOpt;
    protected IAdvDDLOpt advDDLOpt;

    public OracleAdvSimplePageOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt,
                                  IAdvGeoPreOpt advGeoPreOpt, IAdvDDLOpt advDDLOpt) {
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

    /**
     * 执行带参数的统计SQL（复用父类方法）
     */
    protected Long executeCountSqlWithParam(String countSql, GirSqlParam sqlParam) {
        GirAdvOneRow result = getAdvBaseOpt().bSelectOne(countSql, sqlParam);
        return result != null ? result.getLong("COUNT") : 0L;
    }
}
