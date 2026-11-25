package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;

import lombok.Getter;

/** 分别展示输出网格和原始网格，避免只统计融合后的缓存。 */
@Getter
@GaModel(text = "图层缓存统计")
public final class LayerCacheStatistics {
    @GaModelField(text = "输出网格")
    private final TileCacheStatistics outputGrid;

    @GaModelField(text = "原始网格")
    private final TileCacheStatistics originalGrid;

    public LayerCacheStatistics(TileCacheStatistics outputGrid, TileCacheStatistics originalGrid) {
        this.outputGrid = outputGrid;
        this.originalGrid = originalGrid;
    }

    public long getStorageBytes() {
        return outputGrid.getStorageBytes() + originalGrid.getStorageBytes();
    }
}
