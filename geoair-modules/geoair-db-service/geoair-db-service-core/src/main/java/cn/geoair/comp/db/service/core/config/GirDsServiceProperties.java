package cn.geoair.comp.db.service.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "geoair.dynamic.db.service")
@Data
public class GirDsServiceProperties {

    String version = "J8.1.6";

    /** 配置API接口的访问路径 */
    String realApiContext = "/dsApiServer";

    /** 告诉前端的后端代理服务的地址在哪儿，因为有时候后端经过了多从代理，request请求头已经拿不到了 */
    String serviceUrl = "";

    /** 告诉前端的后端代理服务的服务端口在哪儿 */
    Integer servicePort = null;

    /** 配置静态页面的访问路径 */
    final String staticViewContext = "/dsApiView";

    /** 是否启用登录 */
    boolean enableLogin = true;

    /** 默认用户名 */
    String defaultUser = "admin";

    /** 默认密码 */
    String defaultPassword = "geoair";
}
