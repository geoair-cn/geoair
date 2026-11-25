package cn.geoair.map.tile.forge.fuser.fuser;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.provider.MissingTileMeaning;
import cn.geoair.map.tile.forge.fuser.provider.TileReadFailurePolicy;
import cn.geoair.map.tile.forge.fuser.support.FusionTestSource;
import cn.geoair.map.tile.forge.fuser.support.LegacyBytesFuser;
import cn.geoair.map.tile.forge.fuser.support.TileTestImages;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import java.awt.image.BufferedImage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 从真实解码、拼接到缓存写入验证缺片语义，而非只测试计数器。 */
public class TileFusionResultTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final GridSubset grid = GridInitUtils.getWorldGrid3857();

    private GirFuserExec fuser(FusionTestSource source, MissingTileMeaning meaning) {
        BoundingBox bounds = grid.boundsFromRectangle(new long[] {0, 0, 1, 1, 1});
        return new GirFuserExec(
                source,
                ImageMime.png,
                bounds,
                512,
                512,
                meaning,
                TileReadFailurePolicy.KEEP_AVAILABLE);
    }

    @Test
    public void missingSourceKeepsVisiblePixelsAndDoesNotCachePartialResult() throws Exception {
        byte[] red = TileTestImages.solid(0xff0000, "png");
        FusionTestSource source =
                new FusionTestSource(grid, (x, y) -> x == 0 ? new ByteArrayResource(red) : null);
        FileTileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        CacheTileFuserExec cached =
                new CacheTileFuserExec(
                        fuser(source, MissingTileMeaning.UNAVAILABLE), cache, "partial", 1, 0, 0);
        TileFusionResult result = cached.toImageResult();
        assertEquals(TileFusionState.PARTIAL, result.getState());
        assertEquals(2, result.getAvailable());
        assertEquals(2, result.getMissing());
        assertFalse(cache.exists("partial", 1, 0, 0, ImageMime.png));
        BufferedImage image = TileImageUtils.readImage(result.getBytes());
        try {
            assertEquals(0xffff0000, image.getRGB(128, 128));
            assertEquals(0, image.getRGB(384, 128) >>> 24);
        } finally {
            image.flush();
        }
    }

    @Test
    public void knownNoCoverageDoesNotMakeCoveredTilesIncomplete() throws Exception {
        byte[] green = TileTestImages.solid(0x00ff00, "png");
        FusionTestSource source =
                new FusionTestSource(grid, (x, y) -> x == 0 ? new ByteArrayResource(green) : null);
        FileTileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        TileFusionResult result =
                new CacheTileFuserExec(
                                fuser(source, MissingTileMeaning.NO_COVERAGE),
                                cache,
                                "covered",
                                1,
                                0,
                                0)
                        .toImageResult();
        assertEquals(TileFusionState.COMPLETE, result.getState());
        assertEquals(2, result.getNoCoverage());
        assertTrue(cache.exists("covered", 1, 0, 0, ImageMime.png));
    }

    @Test
    public void allMissingCannotPublishAFalseSuccessfulTransparentImage() throws Exception {
        FusionTestSource source = new FusionTestSource(grid, (x, y) -> null);
        TileFusionResult unavailable =
                fuser(source, MissingTileMeaning.UNAVAILABLE).toImageResult();
        assertEquals(TileFusionState.FAILED, unavailable.getState());
        assertNull(unavailable.getBytes());
        assertEquals(
                TileFusionState.NO_COVERAGE,
                fuser(source, MissingTileMeaning.NO_COVERAGE).toImageResult().getState());
    }

    @Test
    public void oneReadFailureDoesNotDiscardOtherTilesWhenExplicitlyAllowed() throws Exception {
        byte[] red = TileTestImages.solid(0xff0000, "png");
        FusionTestSource source =
                new FusionTestSource(
                        grid,
                        (x, y) -> {
                            if (x == 1) throw new IllegalStateException("模拟读取失败");
                            return new ByteArrayResource(red);
                        });
        TileFusionResult result = fuser(source, MissingTileMeaning.UNAVAILABLE).toImageResult();
        assertEquals(TileFusionState.PARTIAL, result.getState());
        assertEquals(2, result.getFailed());
        assertEquals(2, result.getAvailable());
    }

    @Test
    public void corruptImagesAreFailuresInsteadOfCompleteTransparentTiles() throws Exception {
        byte[] red = TileTestImages.solid(0xff0000, "png");
        FusionTestSource source =
                new FusionTestSource(
                        grid, (x, y) -> new ByteArrayResource(x == 1 ? new byte[] {1, 2} : red));
        TileFusionResult result = fuser(source, MissingTileMeaning.UNAVAILABLE).toImageResult();
        assertEquals(TileFusionState.PARTIAL, result.getState());
        assertEquals(2, result.getFailed());
    }

    @Test
    public void cancellationIsNeverSwallowedByPartialImageMode() throws Exception {
        FusionTestSource source =
                new FusionTestSource(
                        grid,
                        (x, y) -> {
                            throw new TileOperationException(TileOperationStatus.CANCELLED, "模拟停止");
                        });
        try {
            fuser(source, MissingTileMeaning.UNAVAILABLE).toImageResult();
            fail("停止应向上传播");
        } catch (TileOperationException expected) {
            assertEquals(TileOperationStatus.CANCELLED, expected.getStatus());
        }
    }

    @Test
    public void strictModePreservesLegacyFailurePropagation() throws Exception {
        FusionTestSource source =
                new FusionTestSource(
                        grid,
                        (x, y) -> {
                            throw new IllegalStateException("模拟失败");
                        });
        GirFuserExec strict =
                new GirFuserExec(
                        source,
                        ImageMime.png,
                        grid.boundsFromIndex(new long[] {0, 0, 0}),
                        256,
                        256);
        assertEquals(TileFusionState.FAILED, strict.toImageResult().getState());
        try {
            strict.toImageBytes();
            fail("旧字节入口应抛出异常");
        } catch (IllegalStateException expected) {
            assertTrue(expected.toString().contains("模拟失败"));
        }
    }

    @Test
    public void ordinaryTypedFailureIsAResultAndNotMistakenForCancellation() throws Exception {
        FusionTestSource source =
                new FusionTestSource(
                        grid,
                        (x, y) -> {
                            throw new TileOperationException(TileOperationStatus.FAILED, "源读取失败");
                        });
        GirFuserExec strict =
                new GirFuserExec(
                        source,
                        ImageMime.png,
                        grid.boundsFromIndex(new long[] {0, 0, 0}),
                        256,
                        256);
        TileFusionResult result = strict.toImageResult();
        assertEquals(TileFusionState.FAILED, result.getState());
        assertNotNull(result.getFailure());
        assertFalse(result.isCacheable());
    }

    @Test
    public void oldSubclassOverridingOnlyBytesRemainsCompatibleWithCacheDecorator()
            throws Exception {
        byte[] png = TileTestImages.solid(0x123456, "png");
        FileTileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        CacheTileFuserExec cached =
                new CacheTileFuserExec(new LegacyBytesFuser(png, false), cache, "legacy", 0, 0, 0);
        assertEquals(TileFusionState.COMPLETE, cached.toImageResult().getState());
        assertArrayEquals(png, cache.get("legacy", 0, 0, 0, ImageMime.png));
    }

    @Test
    public void oldSubclassFailureIsReportedWithoutDereferencingMissingTracker() throws Exception {
        TileFusionResult result = new LegacyBytesFuser(null, true).toImageResult();
        assertEquals(TileFusionState.FAILED, result.getState());
        assertEquals("旧版融合器读取失败", result.getFailure().getMessage());
    }
}
