package cn.geoair.map.dynamic.geoserver.api;

import java.util.Map;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

import cn.geoair.map.dynamic.geoserver.beans.BaseDatastore;
import cn.geoair.map.dynamic.geoserver.beans.PgDatastore;
import cn.geoair.map.dynamic.geoserver.beans.ShpDatastore;
import cn.geoair.map.dynamic.geoserver.enums.DataSourceType;
import cn.geoair.map.dynamic.geoserver.exception.ServicePublishException;

/** GeoServer 数据源（DataStore）管理接口 */
public interface DataStoreManager {

	/**
	 * 创建或获取数据源（推荐：使用抽象数据源配置类）
	 * @param workspace 工作区
	 * @param baseDatastore 抽象数据源配置（PgDatastore/ShpDatastore）
	 * @return 数据源信息
	 * @throws ServicePublishException 创建失败时抛出
	 */
	DataStoreInfo getOrCreateDataStore(WorkspaceInfo workspace, BaseDatastore baseDatastore);

	/**
	 * 创建或获取数据源（兼容：原始 Map 参数）
	 * @param workspace 工作区
	 * @param dataStoreName 数据源名称
	 * @param dataSourceType 数据源类型
	 * @param connectionParams 连接参数
	 * @return 数据源信息
	 * @throws ServicePublishException 创建失败时抛出
	 */
	default DataStoreInfo getOrCreateDataStore(WorkspaceInfo workspace, String dataStoreName,
			DataSourceType dataSourceType, Map<String, String> connectionParams) {
		// 适配逻辑：将 Map 参数转换为 BaseDatastore 子类
		BaseDatastore baseDatastore;
		if (DataSourceType.POSTGIS.equals(dataSourceType)) {
			PgDatastore pgDatastore = new PgDatastore();
			pgDatastore.setName(dataStoreName);
			pgDatastore.setHost(connectionParams.getOrDefault("host", "localhost"));
			pgDatastore.setPort(Integer.parseInt(connectionParams.getOrDefault("port", "5432")));
			pgDatastore.setDatabase(connectionParams.get("database"));
			pgDatastore.setUsername(connectionParams.get("user"));
			pgDatastore.setPassword(connectionParams.get("passwd"));
			pgDatastore.setSchema(connectionParams.getOrDefault("schema", "public"));
			baseDatastore = pgDatastore;
		}
		else if (DataSourceType.SHAPEFILE.equals(dataSourceType)) {
			ShpDatastore shpDatastore = new ShpDatastore();
			shpDatastore.setName(dataStoreName);
			shpDatastore.setShpRootPath(connectionParams.get("url").replace("file:", ""));
			shpDatastore.setCharset(connectionParams.getOrDefault("charset", "GBK"));
			baseDatastore = shpDatastore;
		}
		else {
			throw new ServicePublishException("暂不支持的数据源类型：" + dataSourceType.getDesc());
		}
		return getOrCreateDataStore(workspace, baseDatastore);
	}

	/**
	 * 根据名称获取数据源
	 * @param workspace 工作区
	 * @param dataStoreName 数据源名称
	 * @return 数据源信息（不存在返回null）
	 */
	DataStoreInfo getDataStore(WorkspaceInfo workspace, String dataStoreName);

	/**
	 * 删除数据源（需先删除关联的图层）
	 * @param workspace 工作区
	 * @param dataStoreName 数据源名称
	 * @throws ServicePublishException 删除失败时抛出
	 */
	void deleteDataStore(WorkspaceInfo workspace, String dataStoreName);

	/**
	 * 验证数据源连接是否有效（兼容：原始 Map 参数）
	 * @param dataSourceType 数据源类型
	 * @param connectionParams 连接参数
	 * @return true=有效，false=无效
	 */
	default boolean validateConnection(DataSourceType dataSourceType, Map<String, String> connectionParams) {
		BaseDatastore baseDatastore;
		if (DataSourceType.POSTGIS.equals(dataSourceType)) {
			PgDatastore pgDatastore = new PgDatastore();
			pgDatastore.setHost(connectionParams.getOrDefault("host", "localhost"));
			pgDatastore.setPort(Integer.parseInt(connectionParams.getOrDefault("port", "5432")));
			pgDatastore.setDatabase(connectionParams.get("database"));
			pgDatastore.setUsername(connectionParams.get("user"));
			pgDatastore.setPassword(connectionParams.get("passwd"));
			pgDatastore.setSchema(connectionParams.getOrDefault("schema", "public"));
			baseDatastore = pgDatastore;
		}
		else if (DataSourceType.SHAPEFILE.equals(dataSourceType)) {
			ShpDatastore shpDatastore = new ShpDatastore();
			shpDatastore.setShpRootPath(connectionParams.get("url").replace("file:", ""));
			shpDatastore.setCharset(connectionParams.getOrDefault("charset", "GBK"));
			baseDatastore = shpDatastore;
		}
		else {
			throw new ServicePublishException("暂不支持的数据源类型：" + dataSourceType.getDesc());
		}
		return validateConnection(baseDatastore);
	}

	/**
	 * 验证数据源连接是否有效（推荐：使用抽象数据源配置类）
	 * @param baseDatastore 抽象数据源配置
	 * @return true=有效，false=无效
	 */
	boolean validateConnection(BaseDatastore baseDatastore);

}
