package cn.geoair.base.data.common;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.model.annotation.GaModelField;

/**
 * @author Ray
 */
public enum GemBoolean implements GiVisualValuable<Integer> {
    是(1),
    否(0);

    @GaModelField(isID = true)
    private Integer code;

    GemBoolean(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return this.code;
    }

    @Override
    public String display() {
        return this.name();
    }

    @Override
    public Integer value() {
        return this.code;
    }

    public static GemBoolean findByValue(Integer value, GemBoolean defaultE) {
        return GemUtil.valueOf(GemBoolean.class, value, defaultE);
    }
}
