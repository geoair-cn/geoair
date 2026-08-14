package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.comp.db.service.core.config.GirDsServiceProperties;
import cn.geoair.map.dynamic.tools.GirService;
import jakarta.servlet.http.HttpServletRequest;

public class IPUtil {

    public static final int getOriginPort(HttpServletRequest request) {
        // 可能的代理端口头信息，按优先级排序
        String[] portHeaders = {
            "x-forwarded-port" // 部分反向代理使用
        };

        // 遍历所有可能的端口头，获取第一个有效的端口
        for (String header : portHeaders) {
            String portStr = request.getHeader(header);
            if (portStr != null && !portStr.isEmpty() && !"unknown".equalsIgnoreCase(portStr)) {
                try {
                    return Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    // 端口格式不正确，继续尝试下一个头
                    continue;
                }
            }
        }
        GirDsServiceProperties pxyBeanC = GirService.getPxyBeanC(GirDsServiceProperties.class);
        Integer servicePort = pxyBeanC.getServicePort();
        if (servicePort != null) {
            return servicePort;
        }
        // 如果没有获取到代理传递的端口，则返回本地服务端口
        return request.getServerPort();
    }
}
