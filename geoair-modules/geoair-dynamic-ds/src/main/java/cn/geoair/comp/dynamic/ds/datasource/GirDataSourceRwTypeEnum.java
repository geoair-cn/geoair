package cn.geoair.comp.dynamic.ds.datasource;


import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.model.annotation.GaModelField;

public enum GirDataSourceRwTypeEnum implements GiVisualValuable<String> {
    可读可写("normal"),
    用于读("read_only"),
    ;

    @GaModelField(isID = true)
    private String code;

    GirDataSourceRwTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    @Override
    public String display() {
        return this.name();
    }

    @Override
    public String value() {
        return this.code;
    }
}
