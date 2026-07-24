package cn.geoair.map.dynamic.adv.query.enums;

import org.geotools.geometry.jts.Geometries;
import org.locationtech.jts.geom.*;

import java.io.Serializable;

/**
 * 空间类型枚举
 *
 * @see Geometries
 */
public enum AdvEnumsTypeGeom implements Serializable {
    Point("point", Geometries.POINT, 1, "点", 0),
    LineString("linestring", Geometries.LINESTRING, 2, "线", 2),
    Polygon("polygon", Geometries.POLYGON, 3, "面", 5),
    MultiPoint("multipoint", Geometries.MULTIPOINT, 4, "组合点", 1),
    MultiLineString("multilinestring", Geometries.MULTILINESTRING, 5, "组合线", 4),
    MultiPolygon("multipolygon", Geometries.MULTIPOLYGON, 6, "组合面", 6),
    Geometry("geometry", Geometries.GEOMETRY, 0, "通用空间类型", 999),
    GeometryCollection("geometrycollection", Geometries.GEOMETRYCOLLECTION, 7, "空间集合", 7),
    unknown("unknown", null, 100, "未知", 999),
    ;

    public static AdvEnumsTypeGeom findByGdalValue(Integer value) {
        for (AdvEnumsTypeGeom type : AdvEnumsTypeGeom.values()) {
            if (type.getGdalType() == (value)) {
                return type;
            }
        }
        return unknown;
    }

    /** 小写的名称 */
    private String code;

    /** geotools中对应的类型· */
    private Geometries geotoolsType;

    /** gdal中对应的类型 */
    private int gdalType;

    /** 中文名称 */
    private String chinaName;

    /**
     * geotools也有一个typecode
     *
     * @see Geometry
     */
    private int geotoolsTypeCode;

    public String getChinaName() {
        return chinaName;
    }

    AdvEnumsTypeGeom(
            String code,
            Geometries geotoolsType,
            int gdalType,
            String chinaName,
            int geotoolsTypeCode) {
        this.code = code;
        this.geotoolsType = geotoolsType;
        this.gdalType = gdalType;
        this.chinaName = chinaName;

        this.geotoolsTypeCode = geotoolsTypeCode;
    }

    public String getCode() {
        return code;
    }

    public Geometries getGeotoolsType() {
        return geotoolsType;
    }

    public int getGdalType() {
        return gdalType;
    }

    public int getGeotoolsTypeCode() {
        return geotoolsTypeCode;
    }

    @Override
    public String toString() {
        return code;
    }
}
