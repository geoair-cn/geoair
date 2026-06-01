package cn.geoair.comp.dynamic.ds;

import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;

import javax.sql.DataSource;

/**
 * 动态数据源管理器接口 定义动态数据源的添加、获取、移除、清空等操作
 */
public interface DynamicDataSourceManager {

    /**
     * 清空数据源缓存并释放数据库连接
     */
    void cleanCache();

    /**
     * 检查数据源是否存在
     *
     * @param dataSourceId 数据源ID
     * @return 存在返回true，否则返回false
     */
    boolean containsDataSource(String dataSourceId);

    /**
     * 获取指定ID的数据源
     * 由于实现上是与getOrCreateDataSource一致，但是命名上感觉像是getDataSourceById，所以这个API废弃
     *
     * @param dataSourceId 数据源ID
     * @return DruidDataSource对象
     * @throws RuntimeException 当数据源不存在时抛出异常
     */
    @Deprecated
    AdvDataSourceWrapper getDataSource(String dataSourceId);

    /**
     * 获取指定ID的数据源，如果数据源不存在，就返回Null
     *
     * @param dataSourceId 数据源ID
     * @return DruidDataSource对象
     * @throws RuntimeException 当数据源不存在时抛出异常
     */
    AdvDataSourceWrapper getDataSourceById(String dataSourceId);

    /**
     * 获取指定ID的数据源，如果数据源不存在，就去查询id对应的DataSourceApo进行创建数据源
     *
     * @param dataSourceId 数据源ID
     * @return DruidDataSource对象
     * @throws RuntimeException 当数据源不存在时抛出异常
     */
    AdvDataSourceWrapper getOrCreateDataSource(String dataSourceId);

    /**
     * 添加数据源到管理器
     * 命名不规范，故废弃，移步registerDataSource
     *
     * @param dataSource   数据源对象
     * @param dataSourceId 数据源ID
     */
    @Deprecated
    void putDataSource(DataSource dataSource, String dataSourceId);

    /**
     * 注册数据源到管理器
     *
     * @param dataSource   数据源对象
     * @param dataSourceId 数据源ID
     */
    void registerDataSource(String dataSourceId, DataSource dataSource);

    /**
     * 根据数据源APO对象创建Druid数据源
     *
     * @param dataSourceApo 数据源APO对象
     * @return 创建的 DataSource对象，失败返回null
     */
    AdvDataSourceWrapper getDataSourceByDataSourceApo(DataSourceApo dataSourceApo);

    /**
     * 移除指定ID的数据源并释放连接
     *
     * @param dataSourceId 数据源ID
     * @return 移除成功返回true，数据源不存在返回false
     */
    boolean removeDataSource(String dataSourceId);
}
