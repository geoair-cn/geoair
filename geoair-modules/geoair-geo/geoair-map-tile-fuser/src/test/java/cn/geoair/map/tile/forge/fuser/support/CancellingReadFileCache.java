package cn.geoair.map.tile.forge.fuser.support;

import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.web.mime.GiMimeType;

/** 复现读取完成时任务刚被停止的边界，不依赖不稳定的休眠时序。 */
public final class CancellingReadFileCache extends FileTileCache {
    private final TileCancellationToken token;

    public CancellingReadFileCache(String root, TileCancellationToken token) {
        super(root, 0, true);
        this.token = token;
    }

    @Override
    public byte[] get(String layer, int z, int x, int y, GiMimeType format) {
        byte[] bytes = super.get(layer, z, x, y, format);
        token.cancel();
        return bytes;
    }
}
