package cn.geoair.map.dynamic.geoserver.enums;

/**
 * @author ：张逢吉
 * @date ：Created in 13:35 @description： TODO
 */
public enum DataSourceType {
    POSTGIS("postgis", "PostGIS 数据库"),
    SHAPEFILE("shapefile", "ShapeFile 文件"),
    GEOTIFF("geotiff", "GeoTIFF 栅格文件");

    private final String code;

    private final String desc;

    DataSourceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
