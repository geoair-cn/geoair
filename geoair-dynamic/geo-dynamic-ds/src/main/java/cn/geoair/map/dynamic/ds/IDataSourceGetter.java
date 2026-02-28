package cn.geoair.map.dynamic.ds;

import cn.geoair.map.dynamic.ds.apo.DataSourceApo;
import org.geotools.data.DataStore;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/10/9 10:38 @description： 数据源获取器
 */
public interface IDataSourceGetter {

	/**
	 * 初始化
	 * @param dataSourceApo
	 */
	void initByDataSourceApo(DataSourceApo dataSourceApo);

	/**
	 * 初始化
	 * @param dataSource
	 */
	void initByDataSource(DataSource dataSource);

	/**
	 * 初始化
	 * @param connection
	 */
	void initByConnection(Connection connection);

	String getSchemaName();

	String getDataSourceId();

	/**
	 * 获取数据库链接
	 * @return
	 */
	Connection getConnection();

	/**
	 * 获取数据源的描述对象
	 * @return
	 */
	DataSourceApo getDataSourceApo();

	/**
	 * 获取数据源
	 * @return
	 */
	DataSource getDataSource();

	/**
	 * 获取geotools封装的dataStore
	 * @return
	 */
	DataStore getGeoToolsDataStore();

	/**
	 * 关闭链接
	 * @param connection
	 */
	void connectionClose(Connection connection);

	/**
	 * 关闭资源
	 * @param rs
	 * @param stmt
	 * @param conn
	 */
	void closeResources(ResultSet rs, Statement stmt, Connection conn);

}
