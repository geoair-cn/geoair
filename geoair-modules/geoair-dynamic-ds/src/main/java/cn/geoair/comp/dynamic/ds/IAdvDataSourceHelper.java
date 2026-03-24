package cn.geoair.comp.dynamic.ds;

import javax.sql.DataSource;

import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 10:32 @description： 由上游进行实现
 */
public interface IAdvDataSourceHelper {

	DataSourceApo getDataSourceApoById(String dataSourceId);

	/**
	 * 根据数据源Apo配置创建并返回Druid连接池实例
	 */
	DataSource getDbDataSourceByApo(DataSourceApo dataSourceApo);

}
