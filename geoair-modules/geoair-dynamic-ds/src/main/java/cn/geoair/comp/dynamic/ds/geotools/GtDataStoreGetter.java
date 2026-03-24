package cn.geoair.comp.dynamic.ds.geotools;

import org.geotools.data.DataStore;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * GeoTools数据源获取器接口
 */
public interface GtDataStoreGetter {

	/**
	 * 获取Geotools的DataStore
	 * @param druidDataSource Druid数据源
	 * @param schema 数据库模式名称
	 * @return DataStore对象，失败返回null
	 */
	DataStore getGeotoolsDataStore(DruidDataSource druidDataSource, String schema);

}
