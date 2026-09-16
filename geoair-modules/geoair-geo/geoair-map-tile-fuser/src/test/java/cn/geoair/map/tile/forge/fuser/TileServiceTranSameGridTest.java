package cn.geoair.map.tile.forge.fuser;

import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.SrcType;
import cn.geoair.map.tile.forge.fuser.enums.TileServiceOperation;
import cn.geoair.map.tile.forge.fuser.provider.CachedTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;
import cn.geoair.web.mime.GiMimeType;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/** 同网格直出和原始网格缓存回归测试。 */
public class TileServiceTranSameGridTest {

    @Test
    public void existing3857RequestAutomaticallyUsesSameGridTileAndOriginalCache() {
        FakeLayerTileGetter source = new FakeLayerTileGetter(new byte[]{1, 2, 3});
        MemoryTileCache cache = new MemoryTileCache();
        TestTileServiceTran service = new TestTileServiceTran(webMercatorLayer(), source, cache);

        TileResponse first = service.grid4490ServiceTo3857RequestForTileResponse(
                "web-layer", 2, 1, 1, "image/png");
        TileResponse second = service.grid4490ServiceTo3857RequestForTileResponse(
                "web-layer", 2, 1, 1, "image/png");

        Assert.assertTrue(first.isValid());
        Assert.assertTrue(second.isValid());
        Assert.assertArrayEquals(new byte[]{1, 2, 3}, first.toByteArrays());
        Assert.assertArrayEquals(first.toByteArrays(), second.toByteArrays());
        Assert.assertEquals(TileServiceOperation.SAME_GRID.getCode(), first.getDataSource());
        Assert.assertEquals("EPSG:3857", first.getGridEpsgStr());
        Assert.assertEquals(1, source.requestCount);
        Assert.assertEquals(2, source.lastTmsY);
        Assert.assertEquals(1, cache.putCount);
    }

    @Test
    public void sameGridUriCanDeleteAndRebuildOriginalTileCache() {
        FakeLayerTileGetter source = new FakeLayerTileGetter(new byte[]{4, 5, 6});
        MemoryTileCache cache = new MemoryTileCache();
        TestTileServiceTran service = new TestTileServiceTran(webMercatorLayer(), source, cache);

        String uri = TileServiceTran.buildTileRequestUri(
                TileServiceOperation.SAME_GRID,
                "web-layer", 2, 1, 1, "image/png", false);
        Assert.assertTrue(service.getTileResponse(uri).isValid());

        TileResponse rebuilt = service.sameGridRequestDelCacheForTileResponse(
                "web-layer", 2, 1, 1, "image/png");

        Assert.assertTrue(rebuilt.isValid());
        Assert.assertEquals(2, source.requestCount);
        Assert.assertEquals(1, cache.deleteCount);
        Assert.assertEquals(2, cache.putCount);
    }

    @Test
    public void existing4490RequestAutomaticallyUsesSameGridTileAndOriginalCache() {
        GridSubset grid4490 = GridInitUtils.getTdtGrid4490();
        FakeLayerTileGetter source = new FakeLayerTileGetter(new byte[]{7, 8, 9}, grid4490);
        MemoryTileCache cache = new MemoryTileCache();
        TestTileServiceTran service = new TestTileServiceTran(grid4490Layer(), source, cache);
        BoundingBox bounds = grid4490.boundsFromIndex(new long[]{1, 1, 2});

        TileResponse first = service.buildTileResponse(
                "grid4490-layer", 2, 1, 0, bounds, "image/png", false, 4490);
        TileResponse second = service.buildTileResponse(
                "grid4490-layer", 2, 1, 0, bounds, "image/png", false, 4490);

        Assert.assertTrue(first.isValid());
        Assert.assertTrue(second.isValid());
        Assert.assertArrayEquals(new byte[]{7, 8, 9}, first.toByteArrays());
        Assert.assertEquals(TileServiceOperation.SAME_GRID.getCode(), first.getDataSource());
        Assert.assertEquals("EPSG:4490", first.getGridEpsgStr());
        Assert.assertEquals(1, source.requestCount);
        Assert.assertEquals(1, source.lastTmsY);
        Assert.assertEquals(1, cache.putCount);
    }

    @Test
    public void strictSameGrid4490ApiUsesLayerGrid() {
        FakeLayerTileGetter source = new FakeLayerTileGetter(
                new byte[]{10, 11}, GridInitUtils.getTdtGrid4490());
        TestTileServiceTran service = new TestTileServiceTran(
                grid4490Layer(), source, new MemoryTileCache());

        TileResponse response = service.sameGridRequestForTileResponse(
                "grid4490-layer", 2, 1, 0, "image/png");

        Assert.assertTrue(response.isValid());
        Assert.assertArrayEquals(new byte[]{10, 11}, response.toByteArrays());
        Assert.assertEquals(TileServiceOperation.SAME_GRID.getCode(), response.getDataSource());
        Assert.assertEquals("EPSG:4490", response.getGridEpsgStr());
        Assert.assertEquals(1, source.lastTmsY);
    }

    @Test
    public void strictSameGridApiRejectsFormatConversion() {
        TestTileServiceTran service = new TestTileServiceTran(
                webMercatorLayer(), new FakeLayerTileGetter(new byte[]{7}), new MemoryTileCache());

        TileResponse response = service.sameGridRequestForTileResponse(
                "web-layer", 2, 1, 1, "image/jpeg");

        Assert.assertFalse(response.isSuccess());
        Assert.assertTrue(response.getErrorMessage().contains("不支持格式转换"));
    }

    private PxyLayerInfo webMercatorLayer() {
        return new PxyLayerInfo()
                .setLayerName("web-layer")
                .setSrcType(SrcType.WEB.getCode())
                .setPath("https://tile.example.com/{z}/{x}/{y}.png")
                .setImageType("png")
                .setGridSrid(3857)
                .setEnableCache("true");
    }

    private PxyLayerInfo grid4490Layer() {
        return new PxyLayerInfo()
                .setLayerName("grid4490-layer")
                .setSrcType(SrcType.WEB.getCode())
                .setPath("https://tile.example.com/{z}/{x}/{y}.png")
                .setImageType("png")
                .setGridSrid(4490)
                .setEnableCache("true");
    }

    private static class TestTileServiceTran extends TileServiceTran {
        private final PxyLayerInfo layerInfo;
        private final LayerTileGetter source;
        private final TileCache cache;

        private TestTileServiceTran(PxyLayerInfo layerInfo, LayerTileGetter source, TileCache cache) {
            this.layerInfo = layerInfo;
            this.source = source;
            this.cache = cache;
        }

        @Override
        protected PxyLayerInfo resolveLayerInfo(String layerName) {
            return layerInfo.getLayerName().equals(layerName) ? layerInfo : null;
        }

        @Override
        protected CachedTileGetter createSameGridTileGetter(PxyLayerInfo ignored, String layerName) {
            return new CachedTileGetter(
                    source, layerName + FuserCacheUtils.ORIGINAL_GRID_SUFFIX, cache);
        }
    }

    private static class FakeLayerTileGetter implements LayerTileGetter {
        private final byte[] bytes;
        private final GridSubset sourceGrid;
        private int requestCount;
        private int lastTmsY = -1;

        private FakeLayerTileGetter(byte[] bytes) {
            this(bytes, GridInitUtils.getWorldGrid3857());
        }

        private FakeLayerTileGetter(byte[] bytes, GridSubset sourceGrid) {
            this.bytes = bytes;
            this.sourceGrid = sourceGrid;
        }

        @Override
        public Resource getTileResource(int z, int x, int y) {
            requestCount++;
            lastTmsY = y;
            return new ByteArrayResource(bytes);
        }

        @Override
        public ImageMime getSrcFormat() {
            return ImageMime.png;
        }

        @Override
        public GridSubset getSrcGridSubset() {
            return sourceGrid;
        }
    }

    private static class MemoryTileCache implements TileCache {
        private final Map<String, byte[]> entries = new HashMap<>();
        private int putCount;
        private int deleteCount;

        @Override
        public byte[] get(String layerName, int z, int x, int y, GiMimeType format) {
            return entries.get(key(layerName, z, x, y, format));
        }

        @Override
        public boolean put(String layerName, int z, int x, int y, byte[] data, GiMimeType format) {
            entries.put(key(layerName, z, x, y, format), data);
            putCount++;
            return true;
        }

        @Override
        public boolean deleteLayerCache(String layerName) {
            return false;
        }

        @Override
        public boolean delete(String layerName, Integer z, Integer x) {
            return false;
        }

        @Override
        public boolean delete(String layerName, int z, int x, int y, GiMimeType format) {
            boolean removed = entries.remove(key(layerName, z, x, y, format)) != null;
            if (removed) {
                deleteCount++;
            }
            return removed;
        }

        @Override
        public void clearAll() {
            entries.clear();
        }

        @Override
        public long getTotalSize() {
            return entries.size();
        }

        @Override
        public boolean exists(String layerName, int z, int x, int y, GiMimeType format) {
            return entries.containsKey(key(layerName, z, x, y, format));
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        private String key(String layerName, int z, int x, int y, GiMimeType format) {
            return layerName + "/" + z + "/" + x + "/" + y + "." + format.getFileExtension();
        }
    }
}
