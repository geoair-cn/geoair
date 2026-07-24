package cn.geoair.map.dynamic.adv.query.enums;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.common.GemUtil;
import cn.geoair.base.data.model.annotation.GaModelField;

/**
 * 逻辑操作符枚举
 *
 * <p>用于连接多个条件的逻辑关系
 *
 * @author zhangjun
 */
public enum AdvLogicOperatorEnums implements GiVisualValuable<String> {

    /** 逻辑与 AND */
    AND("201", "AND"),

    /** 逻辑或 OR */
    OR("202", "OR"),

    /** 逻辑非 NOT */
    NOT("203", "NOT");

    @GaModelField(isID = true)
    private final String code;

    private final String value;

    AdvLogicOperatorEnums(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static AdvLogicOperatorEnums getEnumsByCode(String code) {
        if (code == null) return null;
        for (AdvLogicOperatorEnums f : AdvLogicOperatorEnums.values()) {
            if (f.getCode().equals(code)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public String display() {
        return this.name();
    }

    @Override
    public String value() {
        return this.code;
    }

    public static AdvLogicOperatorEnums valueOf(String value, AdvLogicOperatorEnums ifNull) {
        return GemUtil.valueOf(AdvLogicOperatorEnums.class, value, ifNull);
    }
}
