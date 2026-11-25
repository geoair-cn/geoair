package cn.geoair.map.tile.forge.fuser.precache;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.fuser.cache.MBTilesTileCache;
import cn.geoair.map.tile.forge.fuser.encoding.EncodedTileCache;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingPolicy;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingScope;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingSnapshot;
import cn.geoair.map.tile.forge.fuser.encoding.TileImageCodec;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingContext;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingResult;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingState;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesSourceReader;
import cn.geoair.map.tile.forge.fuser.provider.impl.MBTilesTileGetter;
import cn.geoair.map.tile.forge.fuser.support.ConstantTileImageProcessor;
import cn.geoair.map.tile.forge.fuser.support.RasterTestImages;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/** 真实源库和结果库核对原始网格、坐标方向及任务编码，不经过融合。 */
public class OriginalGridPrecachePipelineTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void originalPipelinePreservesSourceGridAndBothStorageOrigins() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            for (int srid : new int[] {3857, 4490})
                for (boolean topLeft : new boolean[] {false, true}) {
                    Path sourceFile =
                            folder.getRoot().toPath().resolve(srid + "_" + topLeft + ".mbtiles");
                    int row = FuserCacheUtils.getStoreY(3, 1, topLeft, srid);
                    try (Connection db =
                                    DriverManager.getConnection(
                                            "jdbc:sqlite:" + sourceFile.toUri());
                            Statement sql = db.createStatement()) {
                        sql.execute(
                                "CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB,PRIMARY KEY(zoom_level,tile_column,tile_row))");
                        try (PreparedStatement insert =
                                db.prepareStatement("INSERT INTO tiles VALUES(3,2,?,?)")) {
                            insert.setInt(1, row);
                            insert.setBytes(2, png);
                            insert.executeUpdate();
                        }
                    }
                    PxyLayerInfo layer =
                            new PxyLayerInfo()
                                    .setLayerName("source")
                                    .setPath(sourceFile.toString())
                                    .setGridSrid(srid)
                                    .setTileRowOrigin(topLeft ? "top-left" : "bottom-left");
                    MBTilesTileCache raw = new MBTilesTileCache(folder.newFolder().toString());
                    try (MbtilesSourceReader reader = new MbtilesSourceReader();
                            MBTilesTileGetter getter = new MBTilesTileGetter(layer, reader)) {
                        BoundingBox box =
                                getter.getSrcGridSubset().boundsFromIndex(new long[] {2, 1, 3});
                        double dx = box.getWidth() / 4, dy = box.getHeight() / 4;
                        Geometry region =
                                new GeometryFactory()
                                        .toGeometry(
                                                new Envelope(
                                                        box.getMinX() + dx,
                                                        box.getMaxX() - dx,
                                                        box.getMinY() + dy,
                                                        box.getMaxY() - dy));
                        if (srid == 3857)
                            region = GirAdvTools.getSridOpt().convert(region, 3857, 4326);
                        OriginalGridPrecache original =
                                new OriginalGridPrecache(
                                        "source", getter, new EncodedTileCache(raw, null));
                        ConstantTileImageProcessor processor =
                                new ConstantTileImageProcessor(
                                        new TileImageProcessingResult(
                                                TileImageProcessingState.PROCESSED,
                                                "test-v1",
                                                1,
                                                1,
                                                png,
                                                "完成"));
                        TilePrecachePipeline pipeline =
                                new TilePrecachePipeline(
                                        raw,
                                        "copy",
                                        processor,
                                        (z, x, y) -> TileImageProcessingContext.ORIGINAL,
                                        new TileEncodingPolicy("PNG", 90),
                                        null);
                        try (TileEncodingScope scope =
                                new TileEncodingSnapshot(
                                                new TileEncodingPolicy("JPEG", 50),
                                                new TileEncodingPolicy())
                                        .bindings(original.getCacheName(), "unused")
                                        .bind()) {
                            List<TilePrecacheProgress> first =
                                    original.execute(
                                            region,
                                            3,
                                            3,
                                            pipeline,
                                            workers,
                                            new TileCancellationToken(),
                                            new TilePrecacheOptions(),
                                            null);
                            assertEquals(1, first.size());
                            assertEquals(1, first.get(0).getTotal());
                            assertEquals(1, first.get(0).getStages().getStored());
                            assertEquals(
                                    "jpeg",
                                    TileImageCodec.detect(
                                            raw.get(
                                                    original.getCacheName(),
                                                    3,
                                                    2,
                                                    1,
                                                    ImageMime.jpeg)));
                            assertEquals(
                                    "png",
                                    TileImageCodec.detect(raw.get("copy", 3, 2, 1, ImageMime.png)));
                            assertArrayEquals(
                                    png, reader.readTile(sourceFile.toString(), 3, 2, row));
                            List<TilePrecacheProgress> reused =
                                    original.execute(
                                            region,
                                            3,
                                            3,
                                            pipeline,
                                            workers,
                                            new TileCancellationToken(),
                                            new TilePrecacheOptions(),
                                            null);
                            assertEquals(1, reused.get(0).getStages().getChecked());
                            assertEquals(1, reused.get(0).getStages().getStored());
                            assertEquals(2, processor.calls.get());
                        }
                    } finally {
                        raw.close();
                    }
                }
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
