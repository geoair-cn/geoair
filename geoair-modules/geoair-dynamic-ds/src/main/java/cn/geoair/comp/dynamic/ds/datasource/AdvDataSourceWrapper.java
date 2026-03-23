package cn.geoair.comp.dynamic.ds.datasource;

import javax.sql.DataSource;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/23 18:12
 * @description： 数据源的包装
 */
public interface AdvDataSourceWrapper extends DataSource {

    /**
     * 是否支持
     *
     * @return
     */
    boolean isSupport();


    /**
     * 获取简单数据源名称
     *
     * @return
     */
    String getSimpleDataSourceName();

    String getJdbcUrl();
}
