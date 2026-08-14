package cn.geoair.map.dynamic.tools.simple.collection.map;


import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.*;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.getter.OptNullBasicTypeFromObjectGetter;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

import org.locationtech.jts.geom.*;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/1/8 15:09 @description： 空间类型的通用get器并适配hutools的
 * OptNullBasicTypeFromObjectGetter
 */
public interface OptNullGeomAndBasicTypeFromObjectGetter
        extends OptNullBasicTypeFromObjectGetter<String> {

    default Class getValueClass(String key) {
        Object obj = getObj(key);
        if (ObjectUtil.isNull(obj)) {
            return null;
        }
        return obj.getClass();
    }

    default Byte[] getByteArray(String key, Byte[] defaultValue) {
        final Object obj = getObj(key);
        if (null == obj) {
            return defaultValue;
        }
        return Convert.toByteArray(obj);
    }

    default Byte[] getByteArray(String key) {
        return getByteArray(key, null);
    }

    default byte[] getPrimitiveByteArray(String key, byte[] defaultValue) {
        final Object obj = getObj(key);
        if (null == obj) {
            return defaultValue;
        }
        return Convert.toPrimitiveByteArray(obj);
    }

    default byte[] getPrimitiveByteArray(String key) {
        return getPrimitiveByteArray(key, null);
    }

    default JSONObject getGeoJsonObj(String key) {
        return getGeoJsonObj(key, null);
    }

    default JSONObject getGeoJsonObj(String key, JSONObject defaultValue) {
        String geometry = getGeoJsonStr(key, null);
        if (geometry == null) {
            return defaultValue;
        }
        return JSON.parseObject(geometry);
    }

    default String getGeoJsonStr(String key) {
        return getGeoJsonStr(key, null);
    }

    default String getGeoJsonStr(String key, String defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        String s = GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToGeoJson(geometry, true);
        if (ObjectUtil.isEmpty(s)) {
            return defaultValue;
        }
        return s;
    }

    default String getWktString(String key) {
        return getWktString(key, null);
    }

    default String getWktString(String key, String defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        String s = GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(geometry, true);
        if (ObjectUtil.isEmpty(s)) {
            return defaultValue;
        }
        return s;
    }

    default String getWkBString(String key) {
        return getWkBString(key, null);
    }

    default String getWkBString(String key, String defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        String s = GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToPgGeometryHex(geometry, true);
        if (ObjectUtil.isEmpty(s)) {
            return defaultValue;
        }
        return s;
    }

    default Geometry getGeometry(String key) {
        Object value = getObj(key);
        Geometry jtsGeom = null;
        if (value instanceof Geometry) {
            jtsGeom = (Geometry) value;
        }
        if (value instanceof String) {
            try {
                value = JSONObject.parseObject((String) value); // 判断是否为geojson字符串
            } catch (Exception e) {
                try {
                    jtsGeom =
                            GirGeoTools.defaultInstance().getFormatOpt()
                                    .wktToJtsGeometry((String) value, true); // 不是geojson字符串就是wkt
                } catch (Exception e1) {
                    jtsGeom =
                            GirGeoTools.defaultInstance().getFormatOpt()
                                    .wkbToJtsGeometry((String) value, true); // 不是wkt字符串就是wbk
                }
            }
        }
        if (value instanceof Map) { // 判断是否为json对象
            JSONObject jsonObject = new JSONObject((Map<String, Object>) value);
            jtsGeom =
                    GirGeoTools.defaultInstance().getFormatOpt()
                            .geojsonToJtsGeometry(jsonObject.toJSONString(), true);
        } else if (GirPostGisTran.isOrgConvert() && GirPostGisOrgTran.isGeometry(value)) {
            return GirPostGisOrgTran.getGeometry(value);
        } else if (GirPostGisTran.isNetConvert() && GirPostGisNetTran.isGeometry(value)) {
            return GirPostGisNetTran.getGeometry(value);
        } else if (GirPostGisTran.isPostGisAvailable() && GirPostGisJdbcTran.isPGobject(value)) { // PGobject 是 PGgeometry的父类
            return GirPostGisJdbcTran.pGobjectToJts(value);
        } else if (GirMysqlTran.isGeomValue(value)) {
            return GirMysqlTran.mysqlBinaryToJtsGeom(value);
        } else if (GirOracleTran.isOracleSpatialAvailable() && GirOracleSpatialTran.isSdoGeometry(value)) {
            return GirOracleSpatialTran.sdoGeometryToJtsGeom(value);
        } else if (GirDMTran.isDmDriverAvailable() && GirDMSpatialTran.isDmdbStruct(value)) {
            return GirDMSpatialTran.dmStructToJtsGeom(value);
        }
        return jtsGeom;
    }

    default LineString getLineString(String key) {
        return getLineString(key, null);
    }

    default LineString getLineString(String key, LineString defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        return (LineString) geometry;
    }

    default MultiLineString getMultiLineString(String key) {
        return getMultiLineString(key, null);
    }

    default MultiLineString getMultiLineString(String key, MultiLineString defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        return (MultiLineString) geometry;
    }

    default Point getPoint(String key) {
        return getPoint(key, null);
    }

    default Point getPoint(String key, Point defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        return (Point) geometry;
    }

    default MultiPoint getMultiPoint(String key) {
        return getMultiPoint(key, null);
    }

    default MultiPoint getMultiPoint(String key, MultiPoint defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        return (MultiPoint) geometry;
    }

    default Polygon getPolygon(String key) {
        return getPolygon(key, null);
    }

    default Polygon getPolygon(String key, Polygon defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        return (Polygon) geometry;
    }

    default MultiPolygon getMultiPolygon(String key) {
        return getMultiPolygon(key, null);
    }

    default MultiPolygon getMultiPolygon(String key, MultiPolygon defaultValue) {
        Geometry geometry = getGeometry(key);
        if (geometry == null) {
            return defaultValue;
        }
        return (MultiPolygon) geometry;
    }
}
