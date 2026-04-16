package cn.geoair.map.dynamic.adv.query.enums;

import cn.geoair.base.data.GiVisualValuable;

/**
 * NULL值处理策略
 */
public enum AdvNullHandling implements GiVisualValuable<String> {
    /**
     * 忽略null值，不加入where条件
     */
    IGNORE,

    /**
     * 包含null值，转为 IS NULL 条件
     */
    INCLUDE,

    /**
     * 遇到null值抛出异常
     */
    THROW
}
