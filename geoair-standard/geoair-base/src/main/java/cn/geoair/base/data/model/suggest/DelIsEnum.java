package cn.geoair.base.data.model.suggest;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/27 10:26
 * @description： 逻辑删除状态
 */
@GaModel(text = "逻辑删除状态")
public enum DelIsEnum implements GiVisualValuable<String> {
    NO("NO", "未删除"),
    YES("YES", "已删除"),
    ;

    @GaModelField(isID = true)
    private final String value;

    @GaModelField(text = "显示名称")
    private final String display;

    DelIsEnum(String value, String display) {
        this.value = value;
        this.display = display;
    }

    @Override
    public String display() {
        return this.display;
    }

    @Override
    public String value() {
        return this.value;
    }
}
