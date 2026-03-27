package cn.geoair.base.data.result.support;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.data.result.GiResult;
import cn.geoair.base.data.result.GirEmAlertType;

@SuppressWarnings("serial")
@GaModel(text = "默认返回结果")
public class GirResult<T> implements GiResult<T> {

    // @GaModelField(text="版本号")
    // protected String version = PRODUCT_VERSION;//版本号

    @GaModelField(text = "状态码")
    private int code = 200; // 状态码

    @GaModelField(text = "提示消息类型", em = GirEmAlertType.class)
    private int alertType = 0; // 消息类型

    @GaModelField(text = "提示消息")
    private String alertMsg; // 提示消息

    @GaModelField(text = "业务数据")
    private T data; // 数据

    public GirResult() {}

    public GirResult(Class<T> cls) {}

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getAlertType() {
        return alertType;
    }

    public void setAlertType(int alertType) {
        this.alertType = alertType;
    }

    public String getAlertMsg() {
        return alertMsg;
    }

    public void setAlertMsg(String alertMsg) {
        this.alertMsg = alertMsg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public GirResult<T> forSuccess() {
        this.andCode(200).andAlertTypeEnum(GirEmAlertType.不弹框0);
        return this;
    }

    @Override
    public GirResult<T> forFailure() {
        this.andAlertTypeEnum(GirEmAlertType.不弹框0)
                .andCode(999)
                .andAlertTypeEnum(GirEmAlertType.需要关闭的提示2);
        return this;
    }

    @Override
    public int code() {
        return this.getCode();
    }

    @Override
    public GiResult<T> andCode(int code) {
        this.setCode(code);
        return this;
    }

    @Override
    public String alertMsg() {
        return this.getAlertMsg();
    }

    @Override
    public GiResult<T> andAlertMsg(String msg) {
        this.setAlertMsg(msg);
        return this;
    }

    @Override
    public int alertType() {
        return this.getAlertType();
    }

    @Override
    public GiResult<T> andAlertType(int alertType) {
        this.setAlertType(alertType);
        return this;
    }

    @Override
    public GiResult<T> andValue(T value) {
        this.setData(value);
        return this;
    }

    @Override
    public T value() {
        return data;
    }
}
