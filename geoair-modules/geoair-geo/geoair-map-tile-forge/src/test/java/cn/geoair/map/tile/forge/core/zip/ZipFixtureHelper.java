package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.SQLiteLayerPerFileDao;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** 只连接 TemporaryFolder 中的索引，支持显式注入索引故障。 */
class ZipFixtureHelper implements GirLayerConfigContextHelper {
    final AtomicInteger lookups = new AtomicInteger();
    boolean broken;

    public Optional<GirLayerConfigContext> getByLayerName(String name) {
        return Optional.empty();
    }

    public Optional<GirLayerConfigContext> getGirLayerConfigContext(
            GirMapTileType type, String layer, String id, String file) {
        return Optional.empty();
    }

    public LayerPerFileDao getLayerPerFileDao(GirLayerConfigContext context) {
        lookups.incrementAndGet();
        if (broken) throw new IllegalStateException("模拟索引不可用");
        return new SQLiteLayerPerFileDao(context.getDataId());
    }

    public Long getLayerPerCacheBatchSize(GirLayerConfigContext context) {
        return 2000L;
    }
}
