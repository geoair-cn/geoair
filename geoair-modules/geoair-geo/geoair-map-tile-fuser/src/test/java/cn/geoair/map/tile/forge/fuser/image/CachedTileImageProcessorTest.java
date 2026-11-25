package cn.geoair.map.tile.forge.fuser.image;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.cache.MBTilesTileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationScope;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.support.CancellingReadFileCache;
import cn.geoair.map.tile.forge.fuser.support.ConstantTileImageProcessor;
import cn.geoair.map.tile.forge.fuser.support.TileTestImages;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 真实 FILE/MBTiles 驱动验收独立输出，不使用管理端控制表。 */
public class CachedTileImageProcessorTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void fileAndMbtilesResultsNeverOverwriteOriginalOrBackfillMissingTiles()
            throws Exception {
        FileTileCache files = new FileTileCache(folder.newFolder("files").toString(), 0, true);
        MBTilesTileCache mbtiles = new MBTilesTileCache(folder.newFolder("mbtiles").toString());
        try {
            verify(files);
            verify(mbtiles);
        } finally {
            mbtiles.close();
        }
    }

    private void verify(TileCache cache) throws Exception {
        byte[] original = TileTestImages.solid(0xff0000, "png"),
                processed = TileTestImages.solid(0x00ff00, "png");
        assertTrue(cache.prepareLayer("source"));
        assertTrue(cache.put("source", 3, 2, 1, original, ImageMime.png));
        assertTrue(cache.prepareLayer("source")); // 再次准备不能清空已有缓存。
        ConstantTileImageProcessor rule =
                new ConstantTileImageProcessor(
                        new TileImageProcessingResult(
                                TileImageProcessingState.PROCESSED,
                                "test-v1",
                                .9,
                                1,
                                processed,
                                "完成"));
        assertEquals(
                TileImageProcessingState.PROCESSED,
                CachedTileImageProcessor.process(
                                cache,
                                "source",
                                cache,
                                "result",
                                3,
                                2,
                                1,
                                ImageMime.png,
                                rule,
                                TileImageProcessingContext.ORIGINAL)
                        .getState());
        assertArrayEquals(original, cache.get("source", 3, 2, 1, ImageMime.png));
        assertArrayEquals(processed, cache.get("result", 3, 2, 1, ImageMime.png));
        assertEquals(
                TileImageProcessingState.SKIPPED,
                CachedTileImageProcessor.process(
                                cache,
                                "source",
                                cache,
                                "result",
                                3,
                                7,
                                7,
                                ImageMime.png,
                                rule,
                                TileImageProcessingContext.ORIGINAL)
                        .getState());
        assertEquals(1, rule.calls.get());
        assertNull(cache.get("source", 3, 7, 7, ImageMime.png));
    }

    @Test
    public void reviewAndUnchangedNeverCreateResultLayers() throws Exception {
        FileTileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        byte[] original = TileTestImages.solid(0xff0000, "png");
        assertTrue(cache.put("source", 1, 0, 0, original, ImageMime.png));
        for (TileImageProcessingState state :
                new TileImageProcessingState[] {
                    TileImageProcessingState.REVIEW, TileImageProcessingState.UNCHANGED
                }) {
            ConstantTileImageProcessor rule =
                    new ConstantTileImageProcessor(
                            new TileImageProcessingResult(state, "test-v1", .4, 0, null, "保留原图"));
            assertEquals(
                    state,
                    CachedTileImageProcessor.process(
                                    cache,
                                    "source",
                                    cache,
                                    "result",
                                    1,
                                    0,
                                    0,
                                    ImageMime.png,
                                    rule,
                                    TileImageProcessingContext.ORIGINAL)
                            .getState());
            assertNull(cache.get("result", 1, 0, 0, ImageMime.png));
            assertArrayEquals(original, cache.get("source", 1, 0, 0, ImageMime.png));
        }
    }

    @Test
    public void failedOutputWriteIsNotCountedAsSuccess() throws Exception {
        FileTileCache source = new FileTileCache(folder.newFolder("source").toString(), 0, true);
        FileTileCache disabled =
                new FileTileCache(folder.newFolder("disabled").toString(), 0, false);
        byte[] png = TileTestImages.solid(0xff0000, "png");
        assertTrue(source.put("source", 1, 0, 0, png, ImageMime.png));
        ConstantTileImageProcessor rule =
                new ConstantTileImageProcessor(
                        new TileImageProcessingResult(
                                TileImageProcessingState.PROCESSED, "test-v1", .9, 1, png, "完成"));
        assertEquals(
                TileImageProcessingState.FAILED,
                CachedTileImageProcessor.process(
                                source,
                                "source",
                                disabled,
                                "result",
                                1,
                                0,
                                0,
                                ImageMime.png,
                                rule,
                                TileImageProcessingContext.ORIGINAL)
                        .getState());
    }

    @Test
    public void rejectsInPlaceProcessingBeforeReadingAndRespectsStop() throws Exception {
        TileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        ConstantTileImageProcessor rule =
                new ConstantTileImageProcessor(
                        new TileImageProcessingResult(
                                TileImageProcessingState.UNCHANGED, "test-v1", 0, 0, null, "原图"));
        try {
            CachedTileImageProcessor.process(
                    cache,
                    "source",
                    cache,
                    "source",
                    1,
                    0,
                    0,
                    ImageMime.png,
                    rule,
                    TileImageProcessingContext.ORIGINAL);
            fail("禁止覆盖原缓存");
        } catch (IllegalArgumentException expected) {
        }
        Thread.currentThread().interrupt();
        try {
            CachedTileImageProcessor.process(
                    cache,
                    "source",
                    cache,
                    "result",
                    1,
                    0,
                    0,
                    ImageMime.png,
                    rule,
                    TileImageProcessingContext.ORIGINAL);
            fail("停止不能继续读写");
        } catch (InterruptedException expected) {
        } finally {
            Thread.interrupted();
        }
        assertEquals(0, rule.calls.get());
    }

    @Test
    public void rejectsStorageNameCollisionEvenIfLayerNamesDiffer() throws Exception {
        TileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        ConstantTileImageProcessor rule =
                new ConstantTileImageProcessor(
                        new TileImageProcessingResult(
                                TileImageProcessingState.UNCHANGED, "test-v1", 0, 0, null, "原图"));
        for (String source : new String[] {"result", "RESULT", "resul."}) {
            String output = source.equals("resul.") ? "resul_" : "result";
            try {
                CachedTileImageProcessor.process(
                        cache,
                        source,
                        cache,
                        output,
                        1,
                        0,
                        0,
                        ImageMime.png,
                        rule,
                        TileImageProcessingContext.ORIGINAL);
                fail("名称净化或 Windows 大小写折叠不能覆盖原库");
            } catch (IllegalArgumentException expected) {
            }
        }
        assertEquals(0, rule.calls.get());
    }

    @Test
    public void stopDuringReadPreventsAlgorithmAndOutputWrite() throws Exception {
        TileCancellationToken token = new TileCancellationToken();
        TileCache source = new CancellingReadFileCache(folder.newFolder("input").toString(), token);
        TileCache output = new FileTileCache(folder.newFolder("output").toString(), 0, true);
        byte[] png = TileTestImages.solid(0xff0000, "png");
        assertTrue(source.put("source", 1, 0, 0, png, ImageMime.png));
        ConstantTileImageProcessor rule =
                new ConstantTileImageProcessor(
                        new TileImageProcessingResult(
                                TileImageProcessingState.PROCESSED, "test-v1", 1, 1, png, "完成"));
        try (TileOperationScope scope = new TileOperationContext(token, 10000).bind()) {
            try {
                CachedTileImageProcessor.process(
                        source,
                        "source",
                        output,
                        "result",
                        1,
                        0,
                        0,
                        ImageMime.png,
                        rule,
                        TileImageProcessingContext.ORIGINAL);
                fail("读取期间停止后不能继续执行算法");
            } catch (TileOperationException expected) {
                assertEquals(TileOperationStatus.CANCELLED, expected.getStatus());
            }
        }
        assertEquals(0, rule.calls.get());
        assertNull(output.get("result", 1, 0, 0, ImageMime.png));
        assertArrayEquals(png, source.get("source", 1, 0, 0, ImageMime.png));
    }
}
