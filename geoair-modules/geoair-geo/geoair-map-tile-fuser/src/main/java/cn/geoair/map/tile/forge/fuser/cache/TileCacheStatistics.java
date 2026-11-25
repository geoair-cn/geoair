package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 存储大小与各级瓦片数据大小分开，未支持的驱动不会伪装成空缓存。 */
@Data
@GaModel(text = "单网格缓存统计")
public class TileCacheStatistics {
    @GaModelField(text = "缓存图层名")
    private String layerName;

    @GaModelField(text = "缓存驱动")
    private CacheStorageKind driver = CacheStorageKind.OTHER;

    @GaModelField(text = "是否支持统计")
    private boolean supported;

    @GaModelField(text = "磁盘文件字节数")
    private long storageBytes;

    @GaModelField(text = "瓦片内容字节数")
    private long tileBytes;

    @GaModelField(text = "瓦片数量")
    private long tileCount;

    @GaModelField(text = "各级别统计")
    private List<TileCacheLevelStatistics> levels = new ArrayList<>();
}
