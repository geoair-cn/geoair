package cn.geoair.map.tile.forge.fuser.encoding;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheQuery;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheStatistics;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.web.mime.GiMimeType;

import java.io.IOException;
import java.util.BitSet;
import java.util.function.Function;

/** 显式装饰已有缓存；原get返回真实存储字节，调用者按需选择格式适配，不改旧驱动。 */
public final class EncodedTileCache implements TileCache {
    private final TileCache delegate;
    private final Function<String, TileEncodingPolicy> policies;

    public EncodedTileCache(TileCache delegate, Function<String, TileEncodingPolicy> policies) {
        if (delegate == null) throw new IllegalArgumentException("缓存不能为空");
        this.delegate = delegate;
        this.policies = policies;
    }

    public TileCache getDelegate() {
        return delegate;
    }

    @Override
    public byte[] get(String layer, int z, int x, int y, GiMimeType format) {
        byte[] raw = delegate.get(layer, z, x, y, format);
        if (raw == null && delegate instanceof FileTileCache)
            raw = delegate.get(layer, z, x, y, ImageMime.png);
        if (raw == null && delegate instanceof FileTileCache)
            raw = delegate.get(layer, z, x, y, ImageMime.jpeg);
        return raw;
    }

    public byte[] getNormalized(String layer, int z, int x, int y, GiMimeType format)
            throws IOException {
        return TileImageCodec.normalize(get(layer, z, x, y, format), format.getInternalName());
    }

    @Override
    public boolean put(String layer, int z, int x, int y, byte[] bytes, GiMimeType format) {
        TileOperationContext.checkCurrent();
        TileEncodingPolicy configured = TileEncodingContext.capture().policy(layer);
        if (configured == null && policies != null) configured = policies.apply(layer);
        TileEncodingPolicy policy =
                configured == null
                        ? new TileEncodingPolicy()
                        : new TileEncodingPolicy(configured.getMode(), configured.getQuality());
        policy.validate();
        if (TileEncodingMode.parse(policy.getMode()) == TileEncodingMode.LEGACY)
            return delegate.put(layer, z, x, y, bytes, format);
        try {
            EncodedTile encoded = TileImageCodec.encode(bytes, policy);
            return putEncoded(delegate, layer, z, x, y, encoded, format);
        } catch (IOException failure) {
            throw new IllegalStateException("瓦片缓存编码失败", failure);
        }
    }

    /** 已编码结果只落盘一次；显式结果策略优先，不能再被外层装饰器重复有损编码。 */
    public static boolean putEncoded(
            TileCache cache,
            String layer,
            int z,
            int x,
            int y,
            EncodedTile encoded,
            GiMimeType fallback) {
        if (cache == null || encoded == null) throw new IllegalArgumentException("缓存和编码结果不能为空");
        while (cache instanceof EncodedTileCache) cache = ((EncodedTileCache) cache).getDelegate();
        GiMimeType actual =
                "jpeg".equals(encoded.getFormat())
                        ? ImageMime.jpeg
                        : "png".equals(encoded.getFormat()) ? ImageMime.png : fallback;
        TileOperationContext.checkCurrent();
        // MBTiles没有扩展名维度，不能删除所谓另一格式而误删同坐标记录。
        return cache instanceof FileTileCache
                ? ((FileTileCache) cache).putRaster(layer, z, x, y, encoded.getBytes(), actual)
                : cache.put(layer, z, x, y, encoded.getBytes(), actual);
    }

    @Override
    public boolean exists(String layer, int z, int x, int y, GiMimeType format) {
        return delegate.exists(layer, z, x, y, format)
                || delegate instanceof FileTileCache
                        && (delegate.exists(layer, z, x, y, ImageMime.png)
                                || delegate.exists(layer, z, x, y, ImageMime.jpeg));
    }

    @Override
    public boolean delete(String layer, int z, int x, int y, GiMimeType format) {
        return delegate instanceof FileTileCache
                ? ((FileTileCache) delegate).deleteRaster(layer, z, x, y, format)
                : delegate.delete(layer, z, x, y, format);
    }

    @Override
    public BitSet findExisting(TileCacheQuery query, TileOperationContext context) {
        BitSet result = delegate.findExisting(query, context);
        if (delegate instanceof FileTileCache)
            for (GiMimeType format : new GiMimeType[] {ImageMime.png, ImageMime.jpeg})
                result.or(
                        delegate.findExisting(
                                new TileCacheQuery(
                                        query.getLayerName(),
                                        query.getZoom(),
                                        query.getRange(),
                                        query.getFrom(),
                                        query.getTo(),
                                        format),
                                context));
        return result;
    }

    @Override
    public TileCacheStatistics statistics(String layer, TileOperationContext context) {
        return delegate.statistics(layer, context);
    }

    @Override
    public boolean prepareLayer(String layer) {
        return delegate.prepareLayer(layer);
    }

    @Override
    public boolean deleteLayerCache(String layer) {
        return delegate.deleteLayerCache(layer);
    }

    @Override
    public boolean delete(String layer, Integer z, Integer x) {
        return delegate.delete(layer, z, x);
    }

    @Override
    public void clearAll() {
        delegate.clearAll();
    }

    @Override
    public long getTotalSize() {
        return delegate.getTotalSize();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }
}
