package cn.geoair.map.dynamic.geoserver.impl;

import cn.geoair.map.dynamic.geoserver.api.LayerPublisher;
import cn.geoair.map.dynamic.geoserver.enums.OgcServiceType;
import cn.geoair.map.dynamic.geoserver.exception.ServicePublishException;

import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMSInfo;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/** PostGIS 数据源的图层发布实现类 */
@Component
public class PostgisLayerPublisher implements LayerPublisher {

    @Autowired private GeoServer geoServer;

    @Override
    public LayerInfo publishLayer(
            WorkspaceInfo workspace,
            DataStoreInfo dataStore,
            String layerName,
            String srs,
            String styleName) {
        try {
            Catalog catalog = geoServer.getCatalog();
            // 1. 校验参数
            Assert.notNull(workspace, "工作区不能为空");
            Assert.notNull(dataStore, "数据源不能为空");
            Assert.hasText(layerName, "图层名称不能为空");
            Assert.hasText(srs, "坐标系不能为空");

            // 2. 检查图层是否已存在
            LayerInfo existingLayer = catalog.getLayerByName(layerName);
            if (existingLayer != null) {
                return existingLayer; // 已存在则直接返回
            }

            // 3. 从数据源加载要素类型
            JDBCDataStore jdbcDataStore = (JDBCDataStore) dataStore.getDataStore(null);
            SimpleFeatureType schema = jdbcDataStore.getSchema(layerName);
            Assert.notNull(schema, "图层 " + layerName + " 不存在或无几何字段");

            // 4. 创建 FeatureTypeInfo
            FeatureTypeInfo featureType = catalog.getFactory().createFeatureType();
            featureType.setName(layerName);
            featureType.setStore(dataStore);
            featureType.setNativeName(layerName);
            featureType.setSRS(srs);
            catalog.add(featureType);

            // 5. 创建 LayerInfo 并关联样式
            LayerInfo layer = catalog.getFactory().createLayer();
            layer.setName(layerName);
            layer.setType(PublishedType.VECTOR);
            layer.setResource(featureType);
            //
            //      StyleInfo style = catalog.getStyleByName(styleName == null ? "point" :
            // styleName);
            //      layer.setDefaultStyle(style);
            catalog.add(layer);

            // 6. 默认启用 WMS/WFS 服务
            enableLayerService(layer, OgcServiceType.WMS, true);
            enableLayerService(layer, OgcServiceType.WFS, true);

            return layer;
        } catch (Exception e) {
            throw new ServicePublishException(
                    "发布 PostGIS 图层 " + layerName + " 失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<LayerInfo> batchPublishLayers(
            WorkspaceInfo workspace,
            DataStoreInfo dataStore,
            List<String> layerNames,
            String srs,
            String styleName) {
        List<LayerInfo> result = new ArrayList<>();
        for (String layerName : layerNames) {
            result.add(publishLayer(workspace, dataStore, layerName, srs, styleName));
        }
        return result;
    }

    @Override
    public void enableLayerService(LayerInfo layer, OgcServiceType serviceType, boolean enabled) {
        try {
            Catalog catalog = geoServer.getCatalog();
            // 以 WMS 为例，更新图层的服务启用状态
            if (OgcServiceType.WMS.equals(serviceType)) {
                WMSInfo wms = geoServer.getService(WMSInfo.class);
                // 此处可扩展：配置图层级别的 WMS 权限/启用状态
                wms.setEnabled(true); // 先确保全局 WMS 启用
                geoServer.save(wms);
            }
            // 其他服务类型（WFS/WCS）类似逻辑
            catalog.save(layer);
        } catch (Exception e) {
            throw new ServicePublishException(
                    "修改图层 "
                            + layer.getName()
                            + " 的 "
                            + serviceType.getDesc()
                            + " 状态失败："
                            + e.getMessage(),
                    e);
        }
    }

    @Override
    public void deleteLayer(WorkspaceInfo workspace, String layerName) {
        try {
            Catalog catalog = geoServer.getCatalog();
            LayerInfo layer = catalog.getLayerByName(layerName);
            if (layer == null) {
                return; // 不存在则直接返回
            }
            // 先删除图层关联的 FeatureType
            FeatureTypeInfo featureType = (FeatureTypeInfo) layer.getResource();
            catalog.remove(featureType);
            // 再删除图层
            catalog.remove(layer);
        } catch (Exception e) {
            throw new ServicePublishException("删除图层 " + layerName + " 失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String getLayerServiceUrl(
            WorkspaceInfo workspace, String layerName, OgcServiceType serviceType) {
        String baseUrl =
                "http://localhost:8080/geoserver/"
                        + workspace.getName()
                        + "/"
                        + serviceType.getCode().toLowerCase();
        return baseUrl
                + "?service="
                + serviceType.getCode()
                + "&version=1.1.0&request=GetCapabilities&layers="
                + layerName;
    }
}
