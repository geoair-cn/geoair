package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;
/** 原图在编码期间被更新，不能用旧结果覆盖新下载。 */
public final class MbtilesTileConflictException extends RuntimeException {
    public MbtilesTileConflictException() {
        super("瓦片已被其他操作更新");
    }
}
