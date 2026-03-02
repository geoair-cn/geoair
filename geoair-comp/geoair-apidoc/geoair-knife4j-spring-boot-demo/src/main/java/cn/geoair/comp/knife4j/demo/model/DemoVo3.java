package cn.geoair.comp.knife4j.demo.model;

import cn.geoair.gtc.base.data.common.GemDel;
import cn.geoair.gtc.base.data.model.annotation.GaModel;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * 演示模型
 *
 * @author zhangjun
 * @date 2022-08-17
 */
@ApiModel(value = "测试demoDemoVo3")
public class DemoVo3 {

	@ApiModelProperty(value = "String 变量")
	private String var1;

	public String getVar1() {
		return var1;
	}

	public void setVar1(String var1) {
		this.var1 = var1;
	}

}
