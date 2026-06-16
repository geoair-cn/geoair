package cn.geoair.map.tile.forge.core.bygwc.layer;



import cn.geoair.map.tile.forge.core.bygwc.config.CacheInfo;
import cn.geoair.map.tile.forge.core.bygwc.config.LODInfo;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSet;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubsetFactory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/17 16:27
 * &#064;description：表示ArcGIS缓存图层的类，用于管理和配置缓存相关属性
 */
@Data
public class ArcGISCacheLayer implements Serializable {

    GridSet gridSet;

    /**
     * 网格子集对象，用于管理网格子集信息
     */
    GridSubset gridSubset;
    /**
     * 缓存信息对象，包含瓦片方案的具体配置
     */
    private transient CacheInfo cacheInfo;

    /**
     * 图层边界框，定义图层的空间范围
     */
    private transient BoundingBox layerBounds;
    /**
     * 图层名称，用于标识图层
     */
    private transient String layerName;


    public ArcGISCacheLayer(String layerName, CacheInfo cacheInfo, BoundingBox layerBounds) {
        this.cacheInfo = cacheInfo;
        this.layerBounds = layerBounds;
        this.layerName = layerName;
        GridSetBuilder gridSetBuilder = new GridSetBuilder();
        this.gridSet = gridSetBuilder.buildGridset(layerName, cacheInfo, layerBounds);
        List<LODInfo> lodInfos = cacheInfo.getTileCacheInfo().getLodInfos();
        Integer zoomStart = lodInfos.get(0).getLevelID();
        Integer zoomStop = lodInfos.get(lodInfos.size() - 1).getLevelID();
        this.gridSubset = GridSubsetFactory.createGridSubSet(gridSet, layerBounds, zoomStart, zoomStop);
    }


}
