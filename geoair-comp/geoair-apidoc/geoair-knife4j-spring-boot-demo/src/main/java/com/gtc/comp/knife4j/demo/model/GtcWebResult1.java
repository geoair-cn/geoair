package com.gtc.comp.knife4j.demo.model;

import cn.geoair.gtc.base.data.model.annotation.GaModel;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.web.data.result.GirWebResult;


/**
 * @author ：张俊
 * @date ：Created in 2022/12/30 17:51
 * @description： TODO
 */
@GaModel(text = "一个新的数据模型")
public class GtcWebResult1<T> extends GirWebResult<T> {

    @GaModelField(text = "一个新的数据项目")
    private T data1;

    public T getData1() {
        return data1;
    }

    public void setData1(T data1) {
        this.data1 = data1;
    }
}
