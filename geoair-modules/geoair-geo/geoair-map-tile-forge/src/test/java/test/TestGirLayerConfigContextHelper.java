package test;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.SQLiteLayerPerFileDao;
import java.util.Optional;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/3 16:20
 * @description： TODO
 */
public class TestGirLayerConfigContextHelper implements GirLayerConfigContextHelper {
    @Override
    public Optional<GirLayerConfigContext> getByLayerName(String layerName) {
        return Optional.empty();
    }

    @Override
    public Optional<GirLayerConfigContext> getGirLayerConfigContext(
            GirMapTileType mapTileType, String layerName, String dataId, String fileName) {
        return Optional.empty();
    }

    @Override
    public LayerPerFileDao getLayerPerFileDao(GirLayerConfigContext layerConfigContext) {
        return new SQLiteLayerPerFileDao(layerConfigContext.getLayerName());
    }

    @Override
    public Long getLayerPerCacheBatchSize(GirLayerConfigContext layerConfigContext) {
        return 3000L;
    }
}
