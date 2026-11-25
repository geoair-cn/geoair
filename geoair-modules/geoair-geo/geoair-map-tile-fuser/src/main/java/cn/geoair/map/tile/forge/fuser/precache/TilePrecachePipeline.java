package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingPolicy;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingContext;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingOutcome;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessor;
import cn.geoair.map.tile.forge.fuser.image.TileImageResultWriter;
import java.util.function.Consumer;

/** 显式启用的下载后处理组合；不自带算法、线程池、业务任务表或重试调度。 */
public final class TilePrecachePipeline {
    private final TileCache resultCache;
    private final String resultLayer;
    private final TileImageProcessor processor;
    private final TileProcessingContextResolver contexts;
    private final TileEncodingPolicy encoding;
    private final Consumer<TilePrecacheTileResult> listener;

    public TilePrecachePipeline(
            TileCache resultCache,
            String resultLayer,
            TileImageProcessor processor,
            TileProcessingContextResolver contexts,
            TileEncodingPolicy encoding,
            Consumer<TilePrecacheTileResult> listener) {
        if (resultCache == null
                || resultLayer == null
                || !resultLayer.matches("[a-zA-Z0-9_-]+")
                || processor == null
                || contexts == null
                || encoding == null) throw new IllegalArgumentException("须提供独立结果缓存、图像规则、缩放与编码策略");
        if (processor.getRuleVersion() == null || processor.getRuleVersion().trim().isEmpty())
            throw new IllegalArgumentException("图像处理规则须提供版本");
        this.resultCache = resultCache;
        this.resultLayer = resultLayer;
        this.processor = processor;
        this.contexts = contexts;
        this.encoding = new TileEncodingPolicy(encoding.getMode(), encoding.getQuality());
        this.encoding.validate();
        this.listener = listener;
    }

    public void validateSource(String sourceLayer) {
        TileImageResultWriter.validateLayers(sourceLayer, resultLayer);
    }

    TileImageProcessingOutcome process(byte[] beforeEncoding, int zoom, int x, int y)
            throws Exception {
        TileImageProcessingContext context = contexts.resolve(zoom, x, y);
        if (context == null) throw new IllegalArgumentException("图像处理缩放未返回结果");
        return TileImageResultWriter.process(
                beforeEncoding, processor, context, resultCache, resultLayer, zoom, x, y, encoding);
    }

    void completed(TilePrecacheTileResult result) {
        if (listener != null) listener.accept(result);
    }
}
