package cn.geoair.map.dynamic.ds;

import cn.geoair.map.dynamic.ds.apo.DataSourceApo;
import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 10:32 @description： 由上游进行实现
 */
public interface IAdvDataSourceHelper {

	DataSourceApo getDataSourceApoById(String dataSourceId);

	/**
	 * 根据数据源Apo配置创建并返回Druid连接池实例
	 */
	DruidDataSource getDbDataSourceByApo(DataSourceApo dataSourceApo);

}
