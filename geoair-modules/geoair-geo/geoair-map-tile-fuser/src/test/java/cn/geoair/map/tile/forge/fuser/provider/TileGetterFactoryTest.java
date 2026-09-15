package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.NotOptTileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.fuser.enums.SrcType;
import cn.geoair.map.tile.forge.fuser.provider.impl.MBTilesTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.google.GoogleLocalFileTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.google.GoogleWebTileGetter;
import cn.geoair.web.mime.GiMimeType;
import org.junit.Assert;
import org.junit.Test;

/** {@link TileGetterFactory} 配置校验、来源路由和缓存边界测试。 */
public class TileGetterFactoryTest {

    @Test
    public void rejectsNullConfigWithFieldContext() {
        assertInvalid(new Runnable() {
            @Override
            public void run() {
                TileGetterFactory.create((PxyLayerInfo) null);
            }
        }, "field=pxyLayerInfo", "value=<null>");
    }

    @Test
    public void rejectsBlankLayerNameAndUnknownSourceType() {
        assertInvalid(new Runnable() {
            @Override
            public void run() {
                TileGetterFactory.create(validWebLayer().setLayerName("  "));
            }
        }, "field=layerName", "value=<blank>");

        assertInvalid(new Runnable() {
            @Override
            public void run() {
                TileGetterFactory.create(validWebLayer().setSrcType("third-party-old-value"));
            }
        }, "layerName=web-layer", "field=srcType", "value=third-party-old-value",
                "当前支持: " + SrcType.getImplementedCodesText());
    }

    @Test
    public void rejectsMissingPathForBuiltInSources() {
        String[] sourceTypes = {
                SrcType.WEB.getCode(), SrcType.LOCAL.getCode(), SrcType.MBTILES.getCode()
        };
        for (final String sourceType : sourceTypes) {
            assertInvalid(new Runnable() {
                @Override
                public void run() {
                    TileGetterFactory.validateConfig(new PxyLayerInfo()
                            .setLayerName(sourceType + "-layer")
                            .setSrcType(sourceType)
                            .setPath(" "));
                }
            }, "layerName=" + sourceType + "-layer", "field=path", sourceType + " 来源");
        }
    }

    @Test
    public void acceptsLegacyOriginConfigAndRoutesWebAndLocalSources() {
        PxyLayerInfo legacyWeb = validWebLayer()
                .setOriginType(OriginType.Google.getMode())
                .setTileRowOrigin(null)
                .setEnableCache("false");
        LayerTileGetter webGetter = TileGetterFactory.create(legacyWeb);
        Assert.assertTrue(webGetter instanceof GoogleWebTileGetter);

        PxyLayerInfo legacyLocal = new PxyLayerInfo()
                .setLayerName("local-layer")
                .setPath("D:/tiles/{z}/{x}/{y}.png")
                .setSrcType(SrcType.LOCAL.getCode())
                .setOriginType(OriginType.TMS.getMode())
                .setGridSrid(3857)
                .setEnableCache("false");
        LayerTileGetter localGetter = TileGetterFactory.create(legacyLocal);
        Assert.assertTrue(localGetter instanceof GoogleLocalFileTileGetter);
    }

    @Test
    public void normalFactoryHonorsCacheSwitch() {
        EnabledTileCache cache = new EnabledTileCache();

        LayerTileGetter uncached = TileGetterFactory.create(
                validWebLayer().setEnableCache("false"), cache);
        Assert.assertFalse(uncached instanceof CachedTileGetter);

        LayerTileGetter cached = TileGetterFactory.create(
                validWebLayer().setEnableCache("true"), cache);
        Assert.assertTrue(cached instanceof CachedTileGetter);
        Assert.assertSame(cache, ((CachedTileGetter) cached).getTileCache());
    }

    @Test
    public void requiredCacheFailsBeforeCreatingCustomGetterWhenLayerCacheIsOff() {
        final PxyLayerInfo customLayer = new PxyLayerInfo()
                .setLayerName("custom-layer")
                .setSrcType(SrcType.CUSTOM.getCode())
                .setEnableCache("false");

        assertInvalid(new Runnable() {
            @Override
            public void run() {
                TileGetterFactory.createRequiredCached(
                        customLayer, new EnabledTileCache(), "custom-layer_original");
            }
        }, "layerName=custom-layer", "field=enableCache", "原始网格缓存任务");
    }

    @Test
    public void requiredCacheRejectsDisabledCacheImplementation() {
        final PxyLayerInfo layer = validWebLayer().setEnableCache("true");

        assertInvalid(new Runnable() {
            @Override
            public void run() {
                TileGetterFactory.createRequiredCached(layer, new NotOptTileCache(), "web-layer_original");
            }
        }, "layerName=web-layer", "field=tileCache", "缓存实现未启用");
    }

    @Test
    public void requiredCacheReturnsTypedGetterForWebLocalAndMbtilesSources() {
        assertRequiredCachedTarget(validWebLayer().setEnableCache("true"), GoogleWebTileGetter.class);

        assertRequiredCachedTarget(new PxyLayerInfo()
                        .setLayerName("local-layer")
                        .setPath("D:/tiles/{z}/{x}/{y}.png")
                        .setSrcType(SrcType.LOCAL.getCode())
                        .setGridSrid(3857)
                        .setEnableCache("1"),
                GoogleLocalFileTileGetter.class);

        CachedTileGetter mbtiles = assertRequiredCachedTarget(new PxyLayerInfo()
                        .setLayerName("mbtiles-layer")
                        .setPath("target/nonexistent-factory-test.mbtiles")
                        .setSrcType(SrcType.MBTILES.getCode())
                        .setGridSrid(3857)
                        .setEnableCache("true"),
                MBTilesTileGetter.class);
        ((MBTilesTileGetter) mbtiles.getTarget()).close();
    }

    @Test
    public void customSourceAllowsEmptyPathDuringValidation() {
        PxyLayerInfo custom = new PxyLayerInfo()
                .setLayerName("custom-layer")
                .setSrcType(SrcType.CUSTOM.getCode())
                .setPath(null);

        Assert.assertSame(custom, TileGetterFactory.validateConfig(custom));
    }

    private CachedTileGetter assertRequiredCachedTarget(PxyLayerInfo layer,
                                                         Class<? extends LayerTileGetter> targetType) {
        EnabledTileCache cache = new EnabledTileCache();
        CachedTileGetter getter = TileGetterFactory.createRequiredCached(
                layer, cache, layer.getLayerName() + "_original");
        Assert.assertTrue(getter.isCacheEnabled());
        Assert.assertSame(cache, getter.getTileCache());
        Assert.assertTrue(targetType.isInstance(getter.getTarget()));
        return getter;
    }

    private PxyLayerInfo validWebLayer() {
        return new PxyLayerInfo()
                .setLayerName("web-layer")
                .setPath("https://tile.example.com/{z}/{x}/{y}.png")
                .setSrcType(SrcType.WEB.getCode())
                .setGridSrid(3857);
    }

    private void assertInvalid(Runnable action, String... messageParts) {
        try {
            action.run();
            Assert.fail("预期抛出 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            for (String messagePart : messageParts) {
                Assert.assertTrue("异常信息缺少: " + messagePart + ", actual=" + e.getMessage(),
                        e.getMessage().contains(messagePart));
            }
        }
    }

    private static class EnabledTileCache implements TileCache {

        @Override
        public byte[] get(String layerName, int z, int x, int y, GiMimeType format) {
            return null;
        }

        @Override
        public boolean put(String layerName, int z, int x, int y, byte[] data, GiMimeType format) {
            return true;
        }

        @Override
        public boolean deleteLayerCache(String layerName) {
            return true;
        }

        @Override
        public boolean delete(String layerName, Integer z, Integer x) {
            return true;
        }

        @Override
        public boolean delete(String layerName, int z, int x, int y, GiMimeType format) {
            return true;
        }

        @Override
        public void clearAll() {
        }

        @Override
        public long getTotalSize() {
            return 0;
        }

        @Override
        public boolean exists(String layerName, int z, int x, int y, GiMimeType format) {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
