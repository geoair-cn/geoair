package cn.geoair.map.dynamic.adv.query.enums;

import org.geotools.geometry.jts.Geometries;

import java.io.Serializable;

/**
 * 结果集中对于空间类型的操作方式
 *
 * @see Geometries
 */
public enum AdvEnumsGeomOpt implements Serializable {

	不做任何操作, 移除, 转换为NULL, 转换为空字符串, 转换成GeoJson, 转换成WKT, 转换成WKB,;

}
