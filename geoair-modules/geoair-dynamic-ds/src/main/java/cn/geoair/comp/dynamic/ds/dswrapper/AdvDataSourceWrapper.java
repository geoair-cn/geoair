package cn.geoair.comp.dynamic.ds.dswrapper;

import javax.sql.DataSource;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/23 18:12 @description： 数据源的包装
 */
public interface AdvDataSourceWrapper extends DataSource {

    static AdvDataSourceWrapper wrap(final DataSource dataSource) {
        return DataSourceWrapperRegistry.getWrapper(dataSource).get();
    }
    /** 获取包装的原始数据源 */
    DataSource getTargetDataSource();

    /**
     * 是否支持
     *
     * @return
     */
    boolean isSupport();

    /**
     * 关闭数据源
     *
     * @return
     */
    boolean close();

    /**
     * 获取简单数据源名称
     *
     * @return
     */
    String getSimpleDataSourceName();

    /**
     * 获取JDBC URL
     *
     * @return
     */
    String getJdbcUrl();

    /**
     * 获取活跃连接数
     *
     * @return
     */
    Integer getActiveCount();
}
