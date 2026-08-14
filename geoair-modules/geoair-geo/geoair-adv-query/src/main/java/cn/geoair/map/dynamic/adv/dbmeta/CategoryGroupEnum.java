package cn.geoair.map.dynamic.adv.dbmeta;

import cn.geoair.base.data.GiVisualValuable;

/**
 * 字段类型的大分组，按语义聚合为一级分类。
 * <p>
 * 每个 {@link CategoryEnum} 属于一个 CategoryGroupEnum。
 * 用于快速判断字段属于哪一大类（如判断是否为数值型、日期型等）。
 *
 * @author zhangjun
 * @date 2026/8/14
 */
public enum CategoryGroupEnum implements GiVisualValuable<String> {

    /** 字符串类型（CHAR、TEXT 等） */
    STRING,
    /** 数值类型（INT、FLOAT 等） */
    NUMBER,
    /** 布尔类型 */
    BOOLEAN,
    /** 二进制类型（BYTES、BLOB） */
    BYTES,
    /** 日期时间类型（DATE、TIME、TIMESTAMP、DATETIME） */
    DATETIME,
    /** 集合类型（如 PostgreSQL 数组） */
    COLLECTION,
    /** 空间几何类型（GEOMETRY、GEOGRAPHY） */
    GEOMETRY,
    /** 时间间隔类型（INTERVAL） */
    INTERVAL,
    /** 其他未分类类型 */
    OTHER,
    /** 无类型（兜底） */
    NONE;

    @Override
    public String value() {
        return this.name();
    }

    @Override
    public String display() {
        return this.name();
    }
}
