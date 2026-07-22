package cn.geoair.map.dynamic.adv.query.dialect.dm;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvSimplePageOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦分页实现类
 */
public class DmAdvSimplePageOpt extends AbstractExecAdvSimplePageOpt {

    protected IAdvGeoPreOpt advGeoPreOpt;
    protected IAdvBaseOpt baseOpt;
    protected IAdvDDLOpt advDDLOpt;

    public DmAdvSimplePageOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt,
                              IAdvGeoPreOpt advGeoPreOpt, IAdvDDLOpt advDDLOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.advDDLOpt = advDDLOpt;
        this.advGeoPreOpt = advGeoPreOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return DmDialectTableNameUtil.getInstance();
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

    protected Long executeCountSqlWithParam(String countSql, GirSqlParam sqlParam) {
        GirAdvOneRow result = getAdvBaseOpt().bSelectOne(countSql, sqlParam);
        return result != null ? result.getLong("COUNT") : 0L;
    }

    public void convertPageOriginalResults(List<GirAdvOneRow> records) {
        for (GirAdvOneRow record : records) {
            record.remove("RN_TEMP");
        }
    }
}
