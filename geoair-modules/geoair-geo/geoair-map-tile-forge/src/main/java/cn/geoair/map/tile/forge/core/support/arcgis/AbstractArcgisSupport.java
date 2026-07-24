package cn.geoair.map.tile.forge.core.support.arcgis;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
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
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.utils.ForgeExecutorUtils;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.hutool.core.lang.Pair;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/2 10:59
 * @description：
 */
public abstract class AbstractArcgisSupport implements ArcgisConfigXmlGetter, ITileStorageSupport {

    protected GirLayerConfigContextHelper contextHelper;

    public AbstractArcgisSupport(GirLayerConfigContextHelper contextHelper) {
        this.contextHelper = contextHelper;
    }

    GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 获取瓦片的缓存信息
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return 瓦片的缓存信息
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public CacheInfo getCacheInfo(GirLayerConfigContext layerConfigContext) throws Exception {
        CacheInfoPersister instance = CacheInfoPersister.getInstance();
        String configXml = getConfigXml(layerConfigContext);
        return instance.load(configXml);
    }

    /**
     * 获取瓦片的 capabilities 文件
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return capabilities 文件
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public String getCapabilities(GirLayerConfigContext layerConfigContext) throws Exception {
        try {

            ArcGISCacheLayer gwcArcGISCacheLayer = getGwcArcGISCacheLayer(layerConfigContext);
            String generate = GetCapabilitiesGenerator.getInstance().generate(gwcArcGISCacheLayer);
            return generate;
        } catch (Exception e) {
            log.error("生成文档异常", e);
            throw e;
        }
    }

    public ArcGISCacheLayer getGwcArcGISCacheLayer(GirLayerConfigContext layerConfigContext)
            throws Exception {
        CacheInfo cacheInfo = getCacheInfo(layerConfigContext);
        BoundingBox boundingBox = getBoundingBox(layerConfigContext);
        String layerName = layerConfigContext.getLayerName();
        return new ArcGISCacheLayer(layerName, cacheInfo, boundingBox);
    }

    /**
     * 获取瓦片的边界信息
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return 瓦片的边界信息
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public BoundingBox getBoundingBox(GirLayerConfigContext layerConfigContext) throws Exception {
        String configCdi = getConfigCdi(layerConfigContext);
        CacheInfoPersister instance = CacheInfoPersister.getInstance();
        return instance.parseLayerBounds(configCdi);
    }

    public void preCacheTiles(
            GirLayerConfigContext layerConfigContext,
            TileCache tileCache,
            ProgressConsumer progressConsumer) {
        // 参数校验
        if (layerConfigContext == null) {
            throw new IllegalArgumentException("layerConfigDto 不能为空");
        }

        ArcGISCacheLayer gwcArcGISCacheLayer;
        GridSet preCacheGridSet;
        GridSubset gridSubset;
        CacheInfo cacheInfo;
        try {
            gwcArcGISCacheLayer = getGwcArcGISCacheLayer(layerConfigContext);
            cacheInfo = gwcArcGISCacheLayer.getCacheInfo();
            gridSubset = gwcArcGISCacheLayer.getGridSubset();
            GridSetBuilder gridSetBuilder = new GridSetBuilder();
            preCacheGridSet =
                    gridSetBuilder.buildGridset(
                            layerConfigContext.getLayerName(),
                            cacheInfo,
                            gwcArcGISCacheLayer.getLayerBounds());
            log.info("GridSet构建完成  ");
        } catch (Exception e) {
            throw new RuntimeException("GridSet构建失败：" + e.getMessage(), e);
        }

        // 统计总瓦片数
        AtomicLong totalTiles = new AtomicLong(0);
        AtomicLong completedTiles = new AtomicLong(0);
        AtomicLong failedTiles = new AtomicLong(0);
        List<LODInfo> lodInfos = cacheInfo.getTileCacheInfo().getLodInfos();
        int gridIndex = 0;
        for (int i = 0; i <= 22; i++) {
            LODInfo lodInfo = lodInfos.get(gridIndex);
            int levelID = lodInfo.getLevelID();
            if (i < levelID) {
                continue;
            }
            Grid grid = preCacheGridSet.getGrid(gridIndex);
            gridIndex++;
            long numTilesHigh = grid.getNumTilesHigh();
            long numTilesWide = grid.getNumTilesWide();
            long tilesInLevel = numTilesHigh * numTilesWide;
            totalTiles.addAndGet(tilesInLevel);

            log.info("层级{}：瓦片总数={}（{}x{}）", i, tilesInLevel, numTilesWide, numTilesHigh);
            Pair<Integer, Integer> xExtremes = getXExtremes(gridSubset, i);
            // 提交瓦片缓存任务
            for (long x = xExtremes.getKey(); x < xExtremes.getValue(); x++) {
                Pair<Integer, Integer> yExtremes = getYExtremes(gridSubset, i);
                for (long y = yExtremes.getKey(); y < yExtremes.getValue(); y++) {
                    final int zoom = i;
                    final long tileX = x;
                    final long tileY = y;
                    ForgeExecutorUtils.getExecutor()
                            .submit(
                                    () -> {
                                        try {
                                            String key =
                                                    tileCache.buildTileCacheKey(
                                                            layerConfigContext.getLayerName(),
                                                            zoom + "",
                                                            tileY + "",
                                                            tileX + "");
                                            TileRequest tileRequest =
                                                    getTileData(
                                                            layerConfigContext,
                                                            zoom + "",
                                                            tileX + "",
                                                            tileY + "");
                                            tileCache.putTile(key, tileRequest, "png");
                                            completedTiles.incrementAndGet();

                                            // 进度日志（每1000个瓦片打印一次）
                                            if (completedTiles.get() % 1000 == 0) {
                                                double progress =
                                                        (completedTiles.get() * 100.0)
                                                                / totalTiles.get();
                                                log.info(
                                                        "缓存进度：{}/{} ({}%)，失败：{}",
                                                        completedTiles.get(),
                                                        totalTiles.get(),
                                                        progress,
                                                        failedTiles.get());
                                            }
                                        } catch (Exception e) {
                                            failedTiles.incrementAndGet();
                                            log.error(
                                                    "缓存瓦片失败: z={}, x={}, y={}",
                                                    zoom,
                                                    tileX,
                                                    tileY,
                                                    e);
                                        }
                                    });
                }
            }
        }
    }

    public Pair<Integer, Integer> getXExtremes(GridSubset gridSubset, int z) {
        try {
            // [minx,miny,maxx,maxy]
            long[] gridCov = gridSubset.getCoverage((int) z);
            return Pair.of((int) gridCov[0], (int) (gridCov[2]));
        } catch (Exception e) {
        }
        return null;
    }

    public Pair<Integer, Integer> getYExtremes(GridSubset gridSubset, int z) {
        try {

            // [minx,miny,maxx,maxy]
            long[] gridCov = gridSubset.getCoverage((int) z);
            return Pair.of((int) gridCov[1], (int) (gridCov[3]));
        } catch (Exception e) {

        }
        return null;
    }
}
