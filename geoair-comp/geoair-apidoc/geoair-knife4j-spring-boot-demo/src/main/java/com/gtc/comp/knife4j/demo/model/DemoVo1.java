package com.gtc.comp.knife4j.demo.model;

import cn.geoair.gtc.base.data.common.GemDel;
import cn.geoair.gtc.base.data.model.annotation.GaModel;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

import java.util.List;

/**
 * 演示模型
 *
 * @author zhangjun
 * @date 2022-08-17
 */
@GaModel(text = "测试demo")
public class DemoVo1 {

	@GaModelField(text = "String 变量")
	private String var1;

	@GaModelField(text = "List 变量")
	private List<String> var2;

	@GaModelField(text = "integer 变量")
	private Integer var3;

	@GaModelField(text = "可选值为枚举的 变量", em = GemDel.class)
	private String var4;

	public String getVar1() {
		return var1;
	}

	public void setVar1(String var1) {
		this.var1 = var1;
	}

	public List<String> getVar2() {
		return var2;
	}

	public void setVar2(List<String> var2) {
		this.var2 = var2;
	}

	public Integer getVar3() {
		return var3;
	}

	public void setVar3(Integer var3) {
		this.var3 = var3;
	}

	public String getVar4() {
		return var4;
	}

	public void setVar4(String var4) {
		this.var4 = var4;
	}

}
