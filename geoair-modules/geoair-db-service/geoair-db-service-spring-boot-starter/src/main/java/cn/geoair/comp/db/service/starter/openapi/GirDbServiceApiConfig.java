package cn.geoair.comp.db.service.starter.openapi;

import cn.geoair.comp.knife4j.ext.core.config.GirOpenApiConfig;
import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;
import cn.hutool.core.collection.ListUtil;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author ：张逢吉
 * @date ：Created in 11:51 @description： 注入api文档的自动装配
 */
@Component
public class GirDbServiceApiConfig extends GirOpenApiConfig {

    @Override
    public List<DocketInfo> getDocketInfos() {
        return ListUtil.of(new DocketInfo("GirDbServiceApi", "cn.geoair.comp.db.service.core"));
    }

    @Override
    public ApiModelInfo getApiModelInfo() {
        return new ApiModelInfo(
                "GirDbServiceApi", "GirDbServiceApi", "GirDbServiceApi", "J8.1.2-SNAPSHOT");
    }
}
