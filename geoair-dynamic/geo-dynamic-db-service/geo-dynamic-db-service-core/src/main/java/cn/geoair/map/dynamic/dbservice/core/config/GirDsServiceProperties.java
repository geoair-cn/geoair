package cn.geoair.map.dynamic.dbservice.core.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "geoair.dynamic.db.service")
@Data
public class GirDsServiceProperties {

    String version = "23.1.2-RC3-SNAPSHOT";

    /** 配置API接口的访问路径 */
    String realApiContext = "/dsApiServer";

    /** 配置静态页面的访问路径 */
    final String staticViewContext = "/dsApiView";

    /** 是否启用登录 */
    boolean enableLogin = true;

    /** 默认用户名 */
    String defaultUser = "admin";

    /** 默认密码 */
    String defaultPassword = "geoair";
}
