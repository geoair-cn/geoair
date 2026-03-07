package cn.geoair.map.dynamic.adv.query.result;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.getter.OptNullBasicTypeFromObjectGetter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.locationtech.jts.geom.*;
import net.postgis.jdbc.PGgeometry;
import org.postgresql.util.PGobject;

import java.util.Map;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/1/8 15:09 @description： 空间类型的通用get器并适配hutools的
 * OptNullBasicTypeFromObjectGetter
 */
public interface OptNullGeomAndBasicTypeFromObjectGetter extends OptNullBasicTypeFromObjectGetter<String> {

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
		String s = GirAdvTools.getFormatOpt().jtsGeometryToGeoJson(geometry, true);
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
		String s = GirAdvTools.getFormatOpt().jtsGeometryToWktString(geometry, true);
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
		String s = GirAdvTools.getFormatOpt().jtsGeometryToPgGeometryHex(geometry, true);
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
				value = JSONObject.parseObject(key); // 判断是否为geojson字符串
			}
			catch (Exception e) {
				try {
					jtsGeom = GirAdvTools.getFormatOpt().wktToJtsGeometry((String) value, true); // 不是geojson字符串就是wkt
				}
				catch (Exception e1) {
					jtsGeom = GirAdvTools.getFormatOpt().wkbToJtsGeometry((String) value, true); // 不是wkt字符串就是wbk
				}
			}
		}
		if (value instanceof Map) { // 判断是否为json对象
			JSONObject jsonObject = new JSONObject((Map<String, Object>) value);
			jtsGeom = GirAdvTools.getFormatOpt().geojsonToJtsGeometry(jsonObject.toJSONString(), true);
		}
		else if (value instanceof PGgeometry) { // 判断是否为pG的空间对象
			PGgeometry pgGeometry = (PGgeometry) value;
			jtsGeom = GirAdvTools.getFormatOpt().pgGeometryToJtsGeometry(pgGeometry, true);
		}
		else if (value instanceof PGobject) { // PGobject 是 PGgeometry的父类
			/**
			 * 若JDBCURL上面显示指定 currentSchema=onemap_tile_builder 。 然而空间类型的元数据（如类型定义）仅存在于
			 * public 中， 驱动在 onemap_tile_builder 下找不到对应的类型定义， 就无法将其识别为 PgGeom， 只能降级为通用的
			 * PgObject 类型。
			 */
			PGobject pObject = (PGobject) value;
			jtsGeom = GirAdvTools.getFormatOpt().wkbToJtsGeometry(StrUtil.toString(pObject), true);
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
