package cn.geoair.map.dynamic.adv.query.supplier;

import cn.geoair.comp.dynamic.ds.base.supplier.GirSysSupplierGetter;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;

/**
 * @author ：张俊
 * @date ：Created in 2026/8/7 17:32
 * @description：
 */
public class GirDataBaseNameGetter implements GirSysSupplierGetter {

    IAdvDDLOpt iAdvDDLOpt;

    public GirDataBaseNameGetter(IAdvDDLOpt iAdvDDLOpt) {

        this.iAdvDDLOpt = iAdvDDLOpt;
    }

    @Override
    public String get() {
        return iAdvDDLOpt.dGetCurrentDataBase();
    }
}
