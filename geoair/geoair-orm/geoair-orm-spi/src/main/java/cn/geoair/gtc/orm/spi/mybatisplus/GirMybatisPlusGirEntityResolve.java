package cn.geoair.gtc.orm.spi.mybatisplus;

import cn.geoair.gtc.orm.spi.GirEntityResolve;
import cn.geoair.gtc.orm.spi.entity.GtcEntityTable;

/**
 * @author ：张俊
 * @date ：Created in 2022/6/30 15:24
 * @description： TODO
 */
public class GirMybatisPlusGirEntityResolve implements GirEntityResolve {
    @Override
    public GtcEntityTable resolveEntity(Class<?> entityClass) {
        return null;
    }

    @Override
    public GtcEntityTable resolveEntity(Object entityidentification) {
        return null;
    }


}
