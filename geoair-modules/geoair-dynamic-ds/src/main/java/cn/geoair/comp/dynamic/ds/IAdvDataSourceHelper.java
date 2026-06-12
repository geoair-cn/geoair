package cn.geoair.comp.dynamic.ds;

import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 10:32 @description： 由上游进行实现
 */
public interface IAdvDataSourceHelper {

    /**
     * 通过数据源ID获取数据源描述对象
     *
     * @param dataSourceId 数据源ID
     * @return 数据源描述对象
     */
    DataSourceApo getDataSourceApoById(String dataSourceId);
}
