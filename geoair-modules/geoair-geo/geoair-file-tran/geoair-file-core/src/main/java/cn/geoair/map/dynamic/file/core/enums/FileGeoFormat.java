package cn.geoair.map.dynamic.file.core.enums;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/12/23 10:18 @description： 支持的文件类型
 */
public enum FileGeoFormat {
    GEOJSON(".geojson"),
    GEOBUF(".geobuf"),
    GEOPKG(".gpkg"),
    CSV(".csv"),
    SHP(".shp"),
    GEOBUFF(".geobuf"),
    FLAT_GEOBUF(".fgb");

    private final String suffix;

    FileGeoFormat(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    /** 根据文件路径自动识别格式 */
    public static FileGeoFormat fromPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        path = path.toLowerCase();
        for (FileGeoFormat format : FileGeoFormat.values()) {
            if (path.endsWith(format.suffix)) {
                return format;
            }
        }
        throw new IllegalArgumentException("无法识别文件格式，支持的格式：" + path);
    }
}
