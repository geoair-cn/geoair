package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

/** 筛选实际字节格式，ANY不依赖metadata.format。 */
public enum MbtilesSourceFormat {
    ANY,
    PNG,
    JPEG
}
