package cn.geoair.map.tile.forge.fuser.encoding;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.cache.MBTilesTileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheQuery;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.provider.FormattedLayerTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.support.RasterTestImages;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EncodedTileCacheTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void explicitGetterAdaptsMixedBytesWithoutChangingTheSourceContract() throws Exception {
        byte[] jpeg = RasterTestImages.image("jpeg", false);
        LayerTileGetter source =
                new LayerTileGetter() {
                    @Override
                    public Resource getTileResource(int z, int x, int y) {
                        return new ByteArrayResource(jpeg);
                    }

                    @Override
                    public ImageMime getSrcFormat() {
                        return ImageMime.jpeg;
                    }

                    @Override
                    public GridSubset getSrcGridSubset() {
                        return null;
                    }
                };
        FormattedLayerTileGetter output = new FormattedLayerTileGetter(source, ImageMime.png);
        assertEquals("png", TileImageCodec.detect(output.getTileResource(3, 2, 1).getByteData()));
        assertEquals(ImageMime.png, output.getSrcFormat());
        assertArrayEquals(jpeg, source.getTileResource(3, 2, 1).getByteData());
    }

    @Test
    public void oversizedBlobIsRejectedBeforeCopyingItIntoTheJavaHeap() throws Exception {
        MBTilesTileCache raw = new MBTilesTileCache(folder.newFolder().toString());
        int limit = TileResourceLimits.getMaxTileBytes();
        try {
            assertTrue(raw.put("oversize", 3, 2, 1, new byte[4096], ImageMime.png));
            TileResourceLimits.setMaxTileBytes(1024);
            assertThrows(
                    IllegalStateException.class, () -> raw.get("oversize", 3, 2, 1, ImageMime.png));
        } finally {
            TileResourceLimits.setMaxTileBytes(limit);
            raw.close();
        }
    }

    @Test
    public void explicitDecoratorPreservesRawContractAndMixedMbtilesCoordinates() throws Exception {
        MBTilesTileCache raw = new MBTilesTileCache(folder.newFolder().toString());
        try {
            EncodedTileCache cache =
                    new EncodedTileCache(raw, layer -> new TileEncodingPolicy("JPEG", 60));
            byte[] png = RasterTestImages.image("png", false),
                    transparent = RasterTestImages.image("png", true);
            assertTrue(cache.put("mixed", 3, 2, 1, png, ImageMime.png));
            assertEquals("jpeg", TileImageCodec.detect(raw.get("mixed", 3, 2, 1, ImageMime.png)));
            assertEquals("jpeg", TileImageCodec.detect(cache.get("mixed", 3, 2, 1, ImageMime.png)));
            assertEquals(
                    "png",
                    TileImageCodec.detect(cache.getNormalized("mixed", 3, 2, 1, ImageMime.png)));
            assertTrue(cache.put("mixed", 3, 3, 1, transparent, ImageMime.png));
            assertArrayEquals(transparent, cache.get("mixed", 3, 3, 1, ImageMime.png));
            assertTrue(cache.exists("mixed", 3, 2, 1, ImageMime.jpeg));
            EncodedTileCache legacy = new EncodedTileCache(raw, null);
            byte[] arbitrary = {1, 2, 3};
            assertTrue(legacy.put("legacy", 3, 2, 1, arbitrary, ImageMime.png));
            assertArrayEquals(arbitrary, legacy.get("legacy", 3, 2, 1, ImageMime.png));
        } finally {
            raw.close();
        }
    }

    @Test
    public void concurrentFileEncodingDoesNotDeleteTheNewTileAndIndexFindsItsExtension()
            throws Exception {
        Path root = folder.newFolder().toPath();
        FileTileCache raw = new FileTileCache(root.toString());
        EncodedTileCache jpeg =
                new EncodedTileCache(raw, layer -> new TileEncodingPolicy("JPEG", 60));
        EncodedTileCache png =
                new EncodedTileCache(raw, layer -> new TileEncodingPolicy("PNG", 75));
        byte[] bytes = RasterTestImages.image("png", false);
        ExecutorService workers = Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                EncodedTileCache next = i % 2 == 0 ? jpeg : png;
                results.add(workers.submit(() -> next.put("mixed", 3, 2, 1, bytes, ImageMime.png)));
            }
            for (Future<Boolean> result : results) assertTrue(result.get());
        } finally {
            workers.shutdownNow();
        }
        assertNotNull(jpeg.get("mixed", 3, 2, 1, ImageMime.png));
        try (Stream<Path> files = Files.walk(root)) {
            assertEquals(1, files.filter(Files::isRegularFile).count());
        }
        TileOperationContext context = new TileOperationContext(new TileCancellationToken(), 5000);
        assertEquals(
                1,
                jpeg.findExisting(
                                new TileCacheQuery(
                                        "mixed",
                                        3,
                                        new RangeApo(2, 2, 1, 1, 3),
                                        0,
                                        1,
                                        ImageMime.png),
                                context)
                        .cardinality());
        assertTrue(jpeg.delete("mixed", 3, 2, 1, ImageMime.png));
        assertFalse(jpeg.exists("mixed", 3, 2, 1, ImageMime.jpeg));
    }
}
