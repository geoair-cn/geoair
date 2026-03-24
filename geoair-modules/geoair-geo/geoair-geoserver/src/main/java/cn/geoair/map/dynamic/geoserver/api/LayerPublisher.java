package cn.geoair.map.dynamic.geoserver.api;

import java.util.List;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;

import cn.geoair.map.dynamic.geoserver.enums.OgcServiceType;
import cn.geoair.map.dynamic.geoserver.exception.ServicePublishException;

/** GeoServer 图层发布接口（核心） */
public interface LayerPublisher {

	/**
	 * 发布单个图层
	 * @param workspace 工作区
	 * @param dataStore 数据源
	 * @param layerName 图层名称（对应数据库表名/文件名称）
	 * @param srs 坐标系（如 EPSG:4326）
	 * @param styleName 样式名称（如 point/polygon，默认样式）
	 * @return 发布后的图层信息
	 * @throws ServicePublishException 发布失败时抛出
	 */
	LayerInfo publishLayer(WorkspaceInfo workspace, DataStoreInfo dataStore, String layerName, String srs,
			String styleName);

	/**
	 * 批量发布图层
	 * @param workspace 工作区
	 * @param dataStore 数据源
	 * @param layerNames 图层名称列表
	 * @param srs 坐标系（统一配置）
	 * @param styleName 样式名称（统一配置）
	 * @return 发布后的图层信息列表
	 * @throws ServicePublishException 批量发布失败时抛出
	 */
	List<LayerInfo> batchPublishLayers(WorkspaceInfo workspace, DataStoreInfo dataStore, List<String> layerNames,
			String srs, String styleName);

	/**
	 * 启用/禁用图层的 OGC 服务
	 * @param layer 图层
	 * @param serviceType OGC服务类型（WMS/WFS）
	 * @param enabled true=启用，false=禁用
	 * @throws ServicePublishException 操作失败时抛出
	 */
	void enableLayerService(LayerInfo layer, OgcServiceType serviceType, boolean enabled);

	/**
	 * 删除图层（同时禁用关联的OGC服务）
	 * @param workspace 工作区
	 * @param layerName 图层名称
	 * @throws ServicePublishException 删除失败时抛出
	 */
	void deleteLayer(WorkspaceInfo workspace, String layerName);

	/**
	 * 获取图层的 OGC 服务访问地址
	 * @param workspace 工作区
	 * @param layerName 图层名称
	 * @param serviceType OGC服务类型
	 * @return 服务地址（如
	 * http://localhost:8080/geoserver/ws/wms?service=WMS&version=1.1.0&request=GetCapabilities）
	 */
	String getLayerServiceUrl(WorkspaceInfo workspace, String layerName, OgcServiceType serviceType);

}
