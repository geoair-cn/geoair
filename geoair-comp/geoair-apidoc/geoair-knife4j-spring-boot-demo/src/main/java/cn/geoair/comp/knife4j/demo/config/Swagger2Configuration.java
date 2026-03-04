package cn.geoair.comp.knife4j.demo.config;

import cn.geoair.comp.knife4j.ext.config.GirSwaggerApiConfig;
import cn.geoair.comp.knife4j.ext.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.model.DocketInfo;
import cn.hutool.core.collection.ListUtil;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Swagger2Configuration implements GirSwaggerApiConfig {



    @Override
    public List<DocketInfo> getDocketInfos() {
        return ListUtil.of(
                new DocketInfo("demo2分组", "cn.geoair.comp.knife4j.demo.controller.group2"),
                new DocketInfo("demo1分组", "cn.geoair.comp.knife4j.demo.controller.group1")
        );
    }

    @Override
    public ApiModelInfo getApiModelInfo() {
        return new ApiModelInfo("demo 在线文档", "demo在线文档", "demo", "1.0");
    }
}
