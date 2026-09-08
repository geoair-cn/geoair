package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

/**
 * V3 静态 MVT 的输出介质类型。
 *
 * @author 张逢吉
 */
public enum V3TileOutputType {

    /** 兼容 V3 初始实现，写入 PostgreSQL 瓦片表。 */
    POSTGRESQL,

    /** 写入本地或共享文件系统的 z/x/y.pbf 目录树。 */
    LOCAL_DIRECTORY,

    /** 写入 S3 兼容对象存储的 z/x/y.pbf 对象树。 */
    S3,

    /** 先写入本地目录，再归档为标准 MBTiles SQLite 文件。 */
    MBTILES,

    /** 先写入本地目录，再归档为 PMTiles V3 单文件。 */
    PMTILES
}
