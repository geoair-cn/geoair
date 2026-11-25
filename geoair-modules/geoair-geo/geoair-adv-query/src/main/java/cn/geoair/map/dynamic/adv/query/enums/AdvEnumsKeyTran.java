package cn.geoair.map.dynamic.adv.query.enums;

import org.geotools.geometry.jts.Geometries;

import java.io.Serializable;

/**
 * 对于key的转换策略
 *
 * @see Geometries
 */
public enum AdvEnumsKeyTran implements Serializable {
    转换成驼峰,
    转换成大小写不敏感,
    不转换,
    ;
}
