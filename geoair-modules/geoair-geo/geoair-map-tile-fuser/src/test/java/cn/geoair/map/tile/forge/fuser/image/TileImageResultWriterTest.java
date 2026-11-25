package cn.geoair.map.tile.forge.fuser.image;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.cache.MBTilesTileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.encoding.EncodedTileCache;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingPolicy;
import cn.geoair.map.tile.forge.fuser.encoding.TileImageCodec;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationScope;
import cn.geoair.map.tile.forge.fuser.support.ConstantTileImageProcessor;
import cn.geoair.map.tile.forge.fuser.support.RasterTestImages;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TileImageResultWriterTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private ConstantTileImageProcessor rule(byte[] bytes) {
        return new ConstantTileImageProcessor(
                new TileImageProcessingResult(
                        TileImageProcessingState.PROCESSED, "test-v1", .9, 1, bytes, "完成"));
    }

    @Test
    public void explicitEncodingWinsOverDecoratorsAndNeverLossilyEncodesTwice() throws Exception {
        FileTileCache raw = new FileTileCache(folder.getRoot().toString(), 0, true);
        TileCache decorated =
                new EncodedTileCache(
                        new EncodedTileCache(raw, name -> new TileEncodingPolicy("JPEG", 10)),
                        name -> new TileEncodingPolicy("JPEG", 95));
        byte[] png = RasterTestImages.image("png", false);
        TileEncodingPolicy selected = new TileEncodingPolicy("JPEG", 60);
        TileImageProcessingOutcome result =
                TileImageResultWriter.process(
                        png,
                        rule(png),
                        TileImageProcessingContext.ORIGINAL,
                        decorated,
                        "result",
                        3,
                        2,
                        1,
                        selected);
        assertTrue(result.isStored());
        assertEquals("jpeg", result.getFormat());
        assertArrayEquals(
                TileImageCodec.encode(png, selected).getBytes(),
                raw.get("result", 3, 2, 1, ImageMime.jpeg));
        assertArrayEquals(
                result.getResult().getBytes(), raw.get("result", 3, 2, 1, ImageMime.jpeg));
        TileImageProcessingOutcome explicitPng =
                TileImageResultWriter.process(
                        png,
                        rule(png),
                        TileImageProcessingContext.ORIGINAL,
                        decorated,
                        "png_result",
                        3,
                        2,
                        1,
                        new TileEncodingPolicy("PNG", 75));
        assertEquals("png", explicitPng.getFormat());
        assertArrayEquals(png, raw.get("png_result", 3, 2, 1, ImageMime.png));
    }

    @Test
    public void cachedEncodedEntrySupportsBothDriversAndRetainsSource() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        FileTileCache files = new FileTileCache(folder.newFolder().toString(), 0, true);
        MBTilesTileCache mbtiles = new MBTilesTileCache(folder.newFolder().toString());
        try {
            for (TileCache cache : new TileCache[] {files, mbtiles}) {
                assertTrue(cache.put("source", 3, 2, 1, png, ImageMime.png));
                TileImageProcessingOutcome result =
                        CachedTileImageProcessor.processEncoded(
                                cache,
                                "source",
                                cache,
                                "result",
                                3,
                                2,
                                1,
                                ImageMime.png,
                                rule(png),
                                TileImageProcessingContext.ORIGINAL,
                                new TileEncodingPolicy("JPEG", 60));
                assertTrue(result.isStored());
                assertEquals(
                        "jpeg",
                        TileImageCodec.detect(cache.get("result", 3, 2, 1, ImageMime.jpeg)));
                assertArrayEquals(png, cache.get("source", 3, 2, 1, ImageMime.png));
                ConstantTileImageProcessor untouched = rule(png);
                assertEquals(
                        TileImageProcessingState.SKIPPED,
                        CachedTileImageProcessor.processEncoded(
                                        cache,
                                        "source",
                                        cache,
                                        "missing",
                                        3,
                                        7,
                                        7,
                                        ImageMime.png,
                                        untouched,
                                        TileImageProcessingContext.ORIGINAL,
                                        new TileEncodingPolicy("PNG", 75))
                                .getResult()
                                .getState());
                assertEquals(0, untouched.calls.get());
            }
        } finally {
            mbtiles.close();
        }
    }

    @Test
    public void transparencyIsPreservedEvenWhenResultPolicyIsJpeg() throws Exception {
        byte[] alpha = RasterTestImages.image("png", true);
        FileTileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        TileImageProcessingOutcome outcome =
                TileImageResultWriter.process(
                        alpha,
                        rule(alpha),
                        TileImageProcessingContext.ORIGINAL,
                        cache,
                        "result",
                        3,
                        2,
                        1,
                        new TileEncodingPolicy("JPEG", 60));
        assertTrue(outcome.isStored());
        assertEquals("png", outcome.getFormat());
        assertArrayEquals(alpha, cache.get("result", 3, 2, 1, ImageMime.png));
    }

    @Test
    public void processingEncodingAndWritingFailuresHaveDifferentStages() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        FileTileCache output = new FileTileCache(folder.newFolder().toString(), 0, true);
        TileImageProcessor broken =
                new TileImageProcessor() {
                    public String getRuleVersion() {
                        return "broken-v1";
                    }

                    public TileImageProcessingResult process(
                            byte[] source, TileImageProcessingContext context) throws IOException {
                        throw new IOException("模拟算法故障");
                    }
                };
        TileImageProcessingOutcome algorithm =
                TileImageResultWriter.process(
                        png,
                        broken,
                        TileImageProcessingContext.ORIGINAL,
                        output,
                        "result",
                        3,
                        2,
                        1,
                        new TileEncodingPolicy("PNG", 75));
        assertEquals(TileImageWriteState.NOT_REQUIRED, algorithm.getWriteState());
        assertNotNull(algorithm.getFailure());
        assertEquals(TileImageProcessingState.FAILED, algorithm.getResult().getState());
        TileImageProcessingOutcome malformed =
                TileImageResultWriter.process(
                        png,
                        rule(new byte[] {1, 2, 3}),
                        TileImageProcessingContext.ORIGINAL,
                        output,
                        "result",
                        3,
                        2,
                        1,
                        new TileEncodingPolicy("JPEG", 60));
        assertEquals(TileImageWriteState.ENCODING_FAILED, malformed.getWriteState());
        FileTileCache disabled = new FileTileCache(folder.newFolder().toString(), 0, false);
        TileImageProcessingOutcome failed =
                TileImageResultWriter.process(
                        png,
                        rule(png),
                        TileImageProcessingContext.ORIGINAL,
                        disabled,
                        "result",
                        3,
                        2,
                        1,
                        new TileEncodingPolicy("JPEG", 60));
        assertEquals(TileImageWriteState.FAILED, failed.getWriteState());
        assertFalse(failed.isStored());
        assertNull(output.get("result", 3, 2, 1, ImageMime.png));
    }

    @Test
    public void cancellationAfterProcessorDoesNotPublishItsResult() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        FileTileCache output = new FileTileCache(folder.getRoot().toString(), 0, true);
        TileCancellationToken stop = new TileCancellationToken();
        TileImageProcessor cancels =
                new TileImageProcessor() {
                    public String getRuleVersion() {
                        return "stop-v1";
                    }

                    public TileImageProcessingResult process(
                            byte[] source, TileImageProcessingContext context) {
                        stop.cancel();
                        return rule(png).process(source, context);
                    }
                };
        try (TileOperationScope scope = new TileOperationContext(stop, 5000).bind()) {
            assertThrows(
                    TileOperationException.class,
                    () ->
                            TileImageResultWriter.process(
                                    png,
                                    cancels,
                                    TileImageProcessingContext.ORIGINAL,
                                    output,
                                    "result",
                                    3,
                                    2,
                                    1,
                                    new TileEncodingPolicy("PNG", 75)));
        }
        assertNull(output.get("result", 3, 2, 1, ImageMime.png));
    }
}
