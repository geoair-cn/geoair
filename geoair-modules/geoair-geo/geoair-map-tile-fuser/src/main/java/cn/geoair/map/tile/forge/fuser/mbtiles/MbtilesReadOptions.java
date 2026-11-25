package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;
import lombok.Data;

/** 读取器创建时复制参数，资源限制由接入方选择。 */
@Data
public class MbtilesReadOptions {
    private int maxOpenFiles = 8;
    private int connectionsPerFile = 2;
    private int poolWaitMillis = 2000;
    private int pageCacheKiB = 2048;
    private int maxTileBytes = TileResourceLimits.getMaxTileBytes();
    private long idleMillis = 60000;
    private long queryTimeoutMillis = 3000;

    public MbtilesReadOptions copy() {
        if (maxOpenFiles < 1 || connectionsPerFile < 1 || poolWaitMillis < 1 || pageCacheKiB < 1
                || maxTileBytes < 1 || idleMillis < 1 || queryTimeoutMillis < 1)
            throw new IllegalArgumentException("MBTiles 读取限制必须大于零");
        Math.multiplyExact(queryTimeoutMillis, 1000000L);
        Math.multiplyExact(idleMillis, 1000000L);
        MbtilesReadOptions copy = new MbtilesReadOptions();
        copy.maxOpenFiles=maxOpenFiles; copy.connectionsPerFile=connectionsPerFile;
        copy.poolWaitMillis=poolWaitMillis; copy.pageCacheKiB=pageCacheKiB; copy.maxTileBytes=maxTileBytes;
        copy.idleMillis=idleMillis; copy.queryTimeoutMillis=queryTimeoutMillis;
        return copy;
    }
}
