package cn.geoair.map.tile.forge.fuser.image;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingPolicy;

/** 只处理已有缓存，结果写到独立图层；不下载、不修改源缓存，不管理业务任务。 */
public final class CachedTileImageProcessor {
    private CachedTileImageProcessor() { }

    /** 显式编码的新入口；旧签名仍按原来的PNG契约执行，也不补下载缺片。 */
    public static TileImageProcessingOutcome processEncoded(TileCache source, String sourceLayer,
            TileCache output, String outputLayer, int z, int x, int y, ImageMime sourceFormat,
            TileImageProcessor processor, TileImageProcessingContext context, TileEncodingPolicy encoding) throws Exception {
        TileImageResultWriter.validateLayers(sourceLayer, outputLayer);
        if (source == null || output == null || sourceFormat == null || processor == null || context == null || encoding == null)
            throw new IllegalArgumentException("处理参数不能为空");
        TileEncodingPolicy policy = new TileEncodingPolicy(encoding.getMode(), encoding.getQuality()); policy.validate();
        checkCancellation();
        byte[] bytes = source.get(sourceLayer, z, x, y, sourceFormat);
        checkCancellation();
        if (bytes == null || bytes.length == 0)
            return new TileImageProcessingOutcome(result(processor, TileImageProcessingState.SKIPPED, "原缓存不存在，不回源下载"),
                    TileImageWriteState.NOT_REQUIRED, null, null);
        return TileImageResultWriter.process(bytes, processor, context, output, outputLayer, z, x, y, policy);
    }

    public static TileImageProcessingResult process(TileCache source, String sourceLayer,
            TileCache output, String outputLayer, int z, int x, int y, ImageMime sourceFormat,
            TileImageProcessor processor, TileImageProcessingContext context) throws Exception {
        if (source == null || output == null || processor == null || context == null || sourceFormat == null
                || sourceLayer == null || sourceLayer.trim().isEmpty() || outputLayer == null
                || !outputLayer.matches("[a-zA-Z0-9_-]+")
                || MbtilesUtils.getSafeFileName(sourceLayer).equalsIgnoreCase(outputLayer))
            throw new IllegalArgumentException("必须提供源缓存和独立命名的输出缓存");
        checkCancellation();
        byte[] bytes = source.get(sourceLayer, z, x, y, sourceFormat);
        checkCancellation();
        if (bytes == null || bytes.length == 0)
            return result(processor, TileImageProcessingState.SKIPPED, "原缓存不存在，不回源下载");
        TileImageProcessingResult processed = processor.process(bytes, context);
        if (processed == null) throw new IllegalStateException("图像处理规则未返回结果");
        checkCancellation();
        if (processed.getState() == TileImageProcessingState.PROCESSED) {
            // 输出统一 PNG，驱动负责建库，写入失败不能计为处理成功。
            byte[] png = TileImageFormats.toPng(processed.getBytes());
            checkCancellation();
            if (!output.put(outputLayer, z, x, y, png, ImageMime.png))
                return result(processor, TileImageProcessingState.FAILED, "结果缓存写入失败");
            return new TileImageProcessingResult(processed.getState(), processed.getRuleVersion(),
                    processed.getScore(), processed.getRegions(), png, processed.getReason());
        }
        return processed;
    }

    private static void checkCancellation() throws InterruptedException {
        TileOperationContext.checkCurrent();
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("图像处理已停止");
    }

    private static TileImageProcessingResult result(TileImageProcessor rule, TileImageProcessingState state, String reason) {
        return new TileImageProcessingResult(state, rule.getRuleVersion(), 0, 0, null, reason);
    }
}
