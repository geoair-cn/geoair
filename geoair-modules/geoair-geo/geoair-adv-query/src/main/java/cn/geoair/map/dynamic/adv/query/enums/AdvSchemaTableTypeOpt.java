package cn.geoair.map.dynamic.adv.query.enums;

import java.io.Serializable;

import org.geotools.geometry.jts.Geometries;

/**
 * 结果集中对于空间类型的操作方式
 *
 * @see Geometries
 */
public enum AdvSchemaTableTypeOpt implements Serializable {

    视图,
    表,
    方法,
    存储过程,
    触发器,
    索引,
    序列,
    数据源连接参数值,
    函数,
    未知,
    ;

}
