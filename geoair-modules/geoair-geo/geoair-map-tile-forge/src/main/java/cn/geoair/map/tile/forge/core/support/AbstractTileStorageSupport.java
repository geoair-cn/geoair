package cn.geoair.map.tile.forge.core.support;

import cn.geoair.map.tile.forge.core.bygwc.config.CacheInfo;
import cn.geoair.map.tile.forge.core.bygwc.config.CacheInfoPersister;
import cn.geoair.map.tile.forge.core.bygwc.config.LODInfo;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.Grid;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSet;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.layer.ArcGISCacheLayer;
import cn.geoair.map.tile.forge.core.bygwc.layer.GridSetBuilder;
import cn.geoair.map.tile.forge.core.bygwc.wmts.GetCapabilitiesGenerator;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.utils.ForgeExecutorUtils;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.hutool.core.lang.Pair;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public abstract class AbstractTileStorageSupport implements ITileStorageSupport {
    /**
     * 获取瓦片数据
     *
     * @param layerConfigContext 瓦片的配置信息
     * @param z              瓦片的级别
     * @param x              瓦片的列号
     * @param y              瓦片的行号
     * @return 瓦片的数据
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public abstract TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception;



    /**
     * 创建TileRequest对象
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return 瓦片的请求对象
     */
    protected TileRequest getTileRequest(GirLayerConfigContext layerConfigContext, String z, String y, String x) {
        TileRequest tileRequest = new TileRequest();
        tileRequest.setStorageType(layerConfigContext.getStorageType());
        tileRequest.setMapTileType(layerConfigContext.getMapTileType());
        tileRequest.setLayerName(layerConfigContext.getLayerName());

        tileRequest.setExists(false);
        tileRequest.setLastModified(0);
        tileRequest.setSize(0);
        tileRequest.setBytes(new byte[0]);
        return tileRequest;
    }

}


