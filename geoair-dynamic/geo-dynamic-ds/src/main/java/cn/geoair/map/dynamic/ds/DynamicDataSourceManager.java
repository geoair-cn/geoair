package cn.geoair.map.dynamic.ds;

import cn.geoair.map.dynamic.ds.apo.DataSourceApo;
import com.alibaba.druid.pool.DruidDataSource;

import org.geotools.data.DataStore;

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
	 * @param dataSourceId 数据源ID
	 * @return 存在返回true，否则返回false
	 */
	boolean containsDataSource(String dataSourceId);

	/**
	 * 获取指定ID的数据源
	 * @param dataSourceId 数据源ID
	 * @return DruidDataSource对象
	 * @throws RuntimeException 当数据源不存在时抛出异常
	 */
	DruidDataSource getDataSource(String dataSourceId);

	/**
	 * 添加数据源到管理器
	 * @param druidDataSource Druid数据源对象
	 * @param dataSourceId 数据源ID
	 */
	void addDataSource(DruidDataSource druidDataSource, String dataSourceId);

	/**
	 * 根据数据源APO对象创建Druid数据源
	 * @param dataSource 数据源APO对象
	 * @return 创建的DruidDataSource对象，失败返回null
	 */
	DruidDataSource getDruidDataSourceByDataSourceApo(DataSourceApo dataSource);

	/**
	 * 获取Geotools的DataStore
	 * @param druidDataSource Druid数据源
	 * @param schema 数据库模式名称
	 * @return DataStore对象，失败返回null
	 */
	DataStore getGeotoolsDataStore(DruidDataSource druidDataSource, String schema);

	/**
	 * 移除指定ID的数据源并释放连接
	 * @param dataSourceId 数据源ID
	 * @return 移除成功返回true，数据源不存在返回false
	 */
	boolean removeDataSource(String dataSourceId);

}
