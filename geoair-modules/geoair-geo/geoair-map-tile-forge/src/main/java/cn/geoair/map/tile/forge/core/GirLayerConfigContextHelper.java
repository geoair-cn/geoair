package cn.geoair.map.tile.forge.core;

import cn.geoair.map.dynamic.tools.GirService;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;

import java.util.Optional;

/** 图层ZIP配置数据访问接口 定义对LayerZipConfig的CRUD操作（实际项目中可基于JPA/MyBatis实现） */
public interface GirLayerConfigContextHelper {

    static GirLayerConfigContextHelper getInstance() {
        return GirService.getPxyBeanC(GirLayerConfigContextHelper.class);
    }

    /**
     * 根据图层名称查询配置
     *
     * @param layerName 图层名称
     * @return 包含LayerZipConfig的Optional（不存在则为empty）
     */
    Optional<GirLayerConfigContext> getByLayerName(String layerName);

    /**
     * @param layerName 图层名称
     * @return 包含LayerZipConfig的Optional（不存在则为empty）
     */
    Optional<GirLayerConfigContext> getGirLayerConfigContext(
            GirMapTileType mapTileType, String layerName, String dataId, String fileName);

    /**
     * 获取图层ZIP文件信息
     *
     * @param layerConfigContext
     * @return
     */
    LayerPerFileDao getLayerPerFileDao(GirLayerConfigContext layerConfigContext);

    /**
     * 获取预缓存的时候，批量插入大小
     *
     * @param layerConfigContext
     * @return
     */
    Long getLayerPerCacheBatchSize(GirLayerConfigContext layerConfigContext);
}
