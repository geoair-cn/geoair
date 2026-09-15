package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import org.junit.Assert;
import org.junit.Test;

/** {@link TileCache} 未命中契约测试。 */
public class NotOptTileCacheTest {

    @Test
    public void disabledCacheReturnsNullOnMiss() {
        TileCache cache = new NotOptTileCache();

        Assert.assertNull(cache.get("layer", 1, 2, 3, ImageMime.png));
        Assert.assertFalse(cache.exists("layer", 1, 2, 3, ImageMime.png));
        Assert.assertFalse(cache.isEnabled());
    }

    @Test
    public void allDisabledCacheImplementationsUseTheSameMissContract() {
        TileCache[] caches = {
                new NotOptTileCache(),
                new FileTileCache("target/cache-contract-file", 0, false),
                new MBTilesTileCache("target/cache-contract-mbtiles", false),
                new PostgresTileCache(null, false)
        };

        for (TileCache cache : caches) {
            Assert.assertNull(cache.get("layer", 1, 2, 3, ImageMime.png));
            Assert.assertFalse(cache.isEnabled());
        }
    }
}
