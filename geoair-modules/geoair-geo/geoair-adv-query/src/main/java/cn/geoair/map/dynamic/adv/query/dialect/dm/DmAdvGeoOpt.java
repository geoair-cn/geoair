package cn.geoair.map.dynamic.adv.query.dialect.dm;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleAdvGeoOpt;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦空间实现类（第一版复用Oracle实现骨架）
 */
public class DmAdvGeoOpt extends OracleAdvGeoOpt {

    public DmAdvGeoOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter, baseOpt, ddlOpt);
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected AdvEnumsTypeGeom getTypeGeomEnum(String nativeGeomType) {
        return super.getTypeGeomEnum(nativeGeomType);
    }
}
