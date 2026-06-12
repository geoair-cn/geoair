package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.base.Gir;
import cn.geoair.comp.db.service.core.config.GirDsServiceProperties;
import cn.geoair.map.dynamic.tools.GirService;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class IPUtil {

    public static void main(String[] args) {
        Gir.log.info("本机IP:" + getIpAddress());
    }

    public static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces =
                    NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                } else {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip != null && ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("IP地址获取失败" + e.toString());
        }
        return "";
    }

    public static final String getOriginIp(HttpServletRequest request) throws IOException {
        // 获取请求主机IP地址,如果通过代理进来，则透过防火墙获取真实IP地址

        String ip = request.getHeader("x-forwarded-for");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } else if (ip.length() > 15) {
            String[] ips = ip.split(",");
            for (int index = 0; index < ips.length; index++) {
                String strIp = (String) ips[index];
                if (!("unknown".equalsIgnoreCase(strIp))) {
                    ip = strIp;
                    break;
                }
            }
        }

        return ip;
    }

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
