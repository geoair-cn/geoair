package cn.geoair.map.tile.forge.fuser.image;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.encoding.EncodedTile;
import cn.geoair.map.tile.forge.fuser.encoding.EncodedTileCache;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingPolicy;
import cn.geoair.map.tile.forge.fuser.encoding.TileImageCodec;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import java.io.IOException;

/** 处理压缩前图片并显式编码结果；未改变和待复核的图片不复制、不覆盖源缓存。 */
public final class TileImageResultWriter {
    private TileImageResultWriter() {}

    public static void validateLayers(String source, String output) {
        if (source == null
                || source.trim().isEmpty()
                || output == null
                || !output.matches("[a-zA-Z0-9_-]+")
                || MbtilesUtils.getSafeFileName(source).equalsIgnoreCase(output))
            throw new IllegalArgumentException("须提供不同的源缓存和独立命名的结果缓存");
    }

    public static TileImageProcessingOutcome process(
            byte[] bytes,
            TileImageProcessor processor,
            TileImageProcessingContext context,
            TileCache output,
            String outputLayer,
            int z,
            int x,
            int y,
            TileEncodingPolicy encoding)
            throws InterruptedException {
        if (bytes == null
                || bytes.length == 0
                || processor == null
                || context == null
                || output == null
                || encoding == null
                || outputLayer == null
                || !outputLayer.matches("[a-zA-Z0-9_-]+"))
            throw new IllegalArgumentException("须提供图片、处理规则、结果缓存和编码策略");
        TileEncodingPolicy policy =
                new TileEncodingPolicy(encoding.getMode(), encoding.getQuality());
        policy.validate();
        check();
        TileImageProcessingResult processed;
        try {
            TileImageFormats.detect(bytes);
            processed = processor.process(bytes, context);
            if (processed == null) throw new IllegalStateException("图像规则未返回结果");
        } catch (TileOperationException stopped) {
            throw stopped;
        } catch (IOException | RuntimeException failure) {
            check();
            return failed(
                    processor.getRuleVersion(),
                    TileImageWriteState.NOT_REQUIRED,
                    null,
                    "图像处理失败",
                    failure);
        }
        check();
        if (processed.getState() != TileImageProcessingState.PROCESSED)
            return new TileImageProcessingOutcome(
                    processed, TileImageWriteState.NOT_REQUIRED, null, null);
        EncodedTile encoded;
        try {
            encoded = TileImageCodec.encode(processed.getBytes(), policy);
            if (!"png".equals(encoded.getFormat()) && !"jpeg".equals(encoded.getFormat()))
                throw new IOException("结果图片仅支持PNG/JPEG");
        } catch (TileOperationException stopped) {
            throw stopped;
        } catch (IOException | RuntimeException failure) {
            check();
            return failed(
                    processed.getRuleVersion(),
                    TileImageWriteState.ENCODING_FAILED,
                    null,
                    "结果图片编码失败",
                    failure);
        }
        check();
        try {
            if (!output.prepareLayer(outputLayer))
                return failed(
                        processed.getRuleVersion(),
                        TileImageWriteState.FAILED,
                        encoded.getFormat(),
                        "结果缓存准备失败",
                        null);
            check();
            if (!EncodedTileCache.putEncoded(output, outputLayer, z, x, y, encoded, ImageMime.png))
                return failed(
                        processed.getRuleVersion(),
                        TileImageWriteState.FAILED,
                        encoded.getFormat(),
                        "结果缓存写入失败",
                        null);
        } catch (TileOperationException stopped) {
            throw stopped;
        } catch (RuntimeException failure) {
            check();
            return failed(
                    processed.getRuleVersion(),
                    TileImageWriteState.FAILED,
                    encoded.getFormat(),
                    "结果缓存写入异常",
                    failure);
        }
        // 已确认落盘后先返回提交事实；调用方再检查总截止时间，不能把真实写入数量抹掉。
        return new TileImageProcessingOutcome(
                new TileImageProcessingResult(
                        TileImageProcessingState.PROCESSED,
                        processed.getRuleVersion(),
                        processed.getScore(),
                        processed.getRegions(),
                        encoded.getBytes(),
                        processed.getReason()),
                TileImageWriteState.STORED,
                encoded.getFormat(),
                null);
    }

    private static TileImageProcessingOutcome failed(
            String rule,
            TileImageWriteState state,
            String format,
            String reason,
            Throwable failure) {
        return new TileImageProcessingOutcome(
                new TileImageProcessingResult(
                        TileImageProcessingState.FAILED, rule, 0, 0, null, reason),
                state,
                format,
                failure);
    }

    private static void check() throws InterruptedException {
        TileOperationContext.checkCurrent();
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("图像处理已停止");
    }
}
