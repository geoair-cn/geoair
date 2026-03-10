package cn.geoair.map.dynamic.dbservice.core.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "geoair.dynamic.db.service")
@Data
public class GirDsServiceProperties {

    /** 配置API接口的访问路径 */
    String realApiContext = "/dsApiServer";

    /** 配置静态页面的访问路径 */
    String staticViewContext = "/dsView";
}
