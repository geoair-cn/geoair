package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.provider.CachedTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.TileGetterFactory;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.Objects;

/** 以读取器真实矩阵规划源瓦片，源读取和缓存写入仅执行一次，不经过融合。 */
public final class OriginalGridPrecache {
    private static final double MERCATOR_MAX_LATITUDE=85.0511287798066;
    private final LayerTileGetter reader;
    private final TileCache cache;
    private final String cacheName;
    private final boolean mercator;

    public OriginalGridPrecache(PxyLayerInfo layer, TileCache cache) {
        this(layer.getLayerName(),TileGetterFactory.createRequiredCached(layer,cache,
                layer.getLayerName()+FuserCacheUtils.ORIGINAL_GRID_SUFFIX));
    }

    private OriginalGridPrecache(String name, CachedTileGetter cached) { this(name,cached.getTarget(),cached.getTileCache()); }

    /** 支持复用业务已创建的源读取器，其生命周期由调用方管理。 */
    public OriginalGridPrecache(String layerName, LayerTileGetter reader, TileCache cache) {
        cacheName=Objects.requireNonNull(layerName)+FuserCacheUtils.ORIGINAL_GRID_SUFFIX;
        this.reader=Objects.requireNonNull(reader);
        this.cache=Objects.requireNonNull(cache);
        this.mercator=reader.getSrcGridSubset().getSRS().getNumber()==3857;
        if(!cache.isEnabled()) throw new IllegalArgumentException("原始网格预缓存需要启用缓存驱动");
    }

    public String getCacheName() { return cacheName; }

    /** 输入范围为 EPSG:4326，级别始终按源读取器解释。 */
    public List<TilePrecacheLevel> plan(Geometry region4326, int minZoom, int maxZoom) {
        if(minZoom<0 || maxZoom<minZoom || maxZoom>30) throw new IllegalArgumentException("原始网格级别范围无效");
        if(region4326==null || region4326.isEmpty() || !region4326.isValid()) throw new IllegalArgumentException("预缓存范围无效");
        Geometry region=region4326.copy();
        if(mercator) {
            region=region.intersection(region.getFactory().toGeometry(new Envelope(-180,180,-MERCATOR_MAX_LATITUDE,MERCATOR_MAX_LATITUDE)));
            if(!region.isEmpty()) region=GirAdvTools.getSridOpt().convert(region,4326,3857);
        }
        List<TilePrecacheLevel> plans=new ArrayList<>();
        if(region.isEmpty()) return plans;
        GridSubset grid=reader.getSrcGridSubset();
        if(minZoom<grid.getZoomStart() || maxZoom>grid.getZoomStop())
            throw new IllegalArgumentException("预缓存级别超出源读取器的矩阵范围");
        Envelope envelope=region.getEnvelopeInternal();
        for(int z=minZoom;z<=maxZoom;z++) {
            long[] range=grid.getCoverageIntersection(z,new BoundingBox(envelope.getMinX(),envelope.getMinY(),envelope.getMaxX(),envelope.getMaxY()));
            if(range==null || range[0]>range[2] || range[1]>range[3]) continue;
            final int zoom=z;
            TileRegionMask mask=new TileRegionMask(region,(x,y)->{
                BoundingBox box=grid.boundsFromIndex(new long[]{x,y,zoom});
                return new Envelope(box.getMinX(),box.getMaxX(),box.getMinY(),box.getMaxY());
            },()->{ if(Thread.currentThread().isInterrupted()) throw new IllegalStateException("任务执行中断"); });
            plans.add(new TilePrecacheLevel(z,new RangeApo(range[0],range[2],range[1],range[3],z),mask));
        }
        return plans;
    }

    public List<TilePrecacheProgress> execute(Geometry region4326, int minZoom, int maxZoom,
            ExecutorService workers, TileCancellationToken cancellation, TilePrecacheOptions options,
            Consumer<TilePrecacheProgress> progress) {
        return SegmentedTilePrecache.execute(cacheName,plan(region4326,minZoom,maxZoom),reader.getSrcFormat(),cache,
                TileContentLoaders.original(reader),workers,cancellation,options,progress);
    }

    /** 原始网格也可逐张接入处理，继续使用源读取器的行号和真实矩阵。 */
    public List<TilePrecacheProgress> execute(Geometry region4326, int minZoom, int maxZoom,
            TilePrecachePipeline pipeline, ExecutorService workers, TileCancellationToken cancellation,
            TilePrecacheOptions options, Consumer<TilePrecacheProgress> progress) {
        return SegmentedTilePrecache.execute(cacheName,plan(region4326,minZoom,maxZoom),reader.getSrcFormat(),cache,
                TileContentLoaders.original(reader),pipeline,workers,cancellation,options,progress);
    }

    /** 原始网格继续按源矩阵和存储行号执行，可复用同一个公平调度器和本地检查点。 */
    public List<TilePrecacheProgress> executeResumable(Geometry region4326, int minZoom, int maxZoom,
            TilePrecachePipeline pipeline, ExecutorService workers, TileCancellationToken cancellation,
            TilePrecacheOptions options, Consumer<TilePrecacheProgress> progress, TilePrecacheBatchGate gate,
            String taskIdentity, TilePrecacheCheckpoint checkpoint, boolean retryFailuresOnly) {
        return SegmentedTilePrecache.executeResumable(cacheName, plan(region4326, minZoom, maxZoom), reader.getSrcFormat(),
                cache, TileContentLoaders.original(reader), pipeline, workers, cancellation, options, progress,
                gate, taskIdentity, checkpoint, retryFailuresOnly);
    }
}
