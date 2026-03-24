package cn.geoair.comp.demo.knife4j.config;

import java.util.List;

import org.springframework.stereotype.Component;

import cn.geoair.comp.knife4j.ext.core.config.GirOpenApiConfig;
import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;

import cn.hutool.core.collection.ListUtil;

@Component
public class Swagger2Configuration extends GirOpenApiConfig {

	@Override
	public List<DocketInfo> getDocketInfos() {
		return ListUtil.of(new DocketInfo("demo2", "cn.geoair.comp.demo.knife4j.controller.group2"),
				new DocketInfo("demo1", "cn.geoair.comp.demo.knife4j.controller.group1"));
	}

	@Override
	public ApiModelInfo getApiModelInfo() {
		return new ApiModelInfo("demo 在线文档", "demo在线文档", "demo", "666666.0");
	}

}
