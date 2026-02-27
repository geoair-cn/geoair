package cn.geoair.comp.knife4j.ext.config;

import cn.geoair.comp.knife4j.ext.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.model.DocketInfo;
import cn.geoair.comp.knife4j.ext.service.SpringAddtionalModelService;
import cn.geoair.comp.knife4j.ext.utils.CreateApiUtil;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.Resource;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:13
 * @description： 父类接口
 */
@Configuration
@EnableSwagger2
public class GtcSwaggerApiConfig {
    @Resource
    SpringAddtionalModelService springAddtionalModelService;



    public Docket createApi(ApiModelInfo apiModelInfo, DocketInfo docketInfo) {
        return CreateApiUtil.createGroup(apiModelInfo, docketInfo, springAddtionalModelService);
    }
}
