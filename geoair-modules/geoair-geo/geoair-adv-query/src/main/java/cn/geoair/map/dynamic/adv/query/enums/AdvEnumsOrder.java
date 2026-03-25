package cn.geoair.map.dynamic.adv.query.enums;

import cn.hutool.core.util.ObjectUtil;
import java.io.Serializable;

/**
 * @author ：zhangjun
 * @date ：Created in 2023/6/11 13:43 @description： 排序枚举
 */
public enum AdvEnumsOrder implements Serializable {
    升序("ASC"),
    降序("DESC");

    private String value;

    public String getValue() {
        return value;
    }

    AdvEnumsOrder(String value) {
        this.value = value;
    }

    public static AdvEnumsOrder getEnumsByValue(String value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        value = value.toUpperCase();
        for (AdvEnumsOrder f : AdvEnumsOrder.values()) {
            if (f.getValue().equals(value)) {
                return f;
            }
        }
        return null;
    }

    public static AdvEnumsOrder getEnumsByName(String name) {
        for (AdvEnumsOrder f : AdvEnumsOrder.values()) {
            if (f.name().equals(name)) {
                return f;
            }
        }
        return null;
    }
}
