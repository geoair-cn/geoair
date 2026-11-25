package cn.geoair.map.tile.forge.fuser.provider.impl;

import cn.geoair.base.runtime.GutilShutdownHook;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesSourceReader;
import cn.geoair.map.tile.forge.fuser.provider.BaseTileGetter;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import java.io.IOException;
import java.sql.SQLException;

/** 请求级 Getter 仅持有配置，源文件由可共享、有界的只读读取器管理。 */
public class MBTilesTileGetter extends BaseTileGetter implements AutoCloseable {
    private static final MbtilesSourceReader SHARED = new MbtilesSourceReader();
    static { GutilShutdownHook.getInstance().registerTask(SHARED::close); }
    private final MbtilesSourceReader reader;
    private final String path;
    private volatile boolean closed;

    public MBTilesTileGetter(PxyLayerInfo layer) { this(layer, SHARED); }

    /** 读取器生命周期归接入方，可配置连接数、超时和内存限制。 */
    public MBTilesTileGetter(PxyLayerInfo layer, MbtilesSourceReader reader) {
        super(layer);
        if (reader == null || layer.getPath() == null || layer.getPath().trim().isEmpty())
            throw new IllegalArgumentException("MBTiles 路径和读取器不能为空");
        this.path = layer.getPath().trim();
        this.reader = reader;
    }

    @Override public Resource getTileResource(int z, int x, int y) {
        if (closed) throw new IllegalStateException("MBTiles Getter 已关闭");
        int row = FuserCacheUtils.getStoreY(z, y, getLayerInfo().getTileRowOriginEnums().isTopLeft(),
                FuserCacheUtils.getCacheGridSrid(getLayerInfo()));
        try {
            byte[] bytes = reader.readTile(path, z, x, row);
            return bytes == null ? null : new ByteArrayResource(bytes);
        } catch (IOException | SQLException e) {
            // 读库故障不能作为缺片参与融合，否则会把临时错误固化到输出缓存。
            throw new IllegalStateException("无法读取 MBTiles 源瓦片", e);
        }
    }

    @Override public void close() { closed = true; }
}